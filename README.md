# osquery-process-port

[osquery](https://osquery.io/) allows us to capture data from the operating system. The queries are expressed as SQL, and can combine elements from multiple sources. For example, the query below tells us which processes are communicating with external hosts in one simple query:

    [root@cp01 ~]# osqueryi
    Using a virtual database. Need help, type '.help'
    osquery> select u.username, p.pid, p.name, pos.local_address, pos.local_port, pos.remote_address, pos.remote_port from processes p join users u on u.uid = p.uid join process_open_sockets pos on pos.pid=p.pid where pos.remote_port != '0';
    +--------------------+-------+---------------+------------------+------------+------------------+-------------+
    | username           | pid   | name          | local_address    | local_port | remote_address   | remote_port |
    +--------------------+-------+---------------+------------------+------------+------------------+-------------+
    | cp-kafka-connect   | 12962 | java          | 10.0.1.41        | 49650      | 10.0.1.43        | 9092        |
    | cp-kafka-connect   | 12962 | java          | 10.0.1.41        | 41654      | 10.0.1.42        | 9092        |
    | cp-kafka-rest      | 1755  | java          | 10.0.1.41        | 55646      | 10.0.1.32        | 2181        |
    | cp-ksql            | 1756  | java          | 10.0.1.41        | 39446      | 10.0.1.42        | 9092        |
    | cp-ksql            | 1756  | java          | 10.0.1.41        | 39598      | 10.0.1.42        | 9092        |
     ...
    | root               | 27812 | filebeat      | 10.0.1.41        | 40040      | 10.0.1.43        | 9092        |
    | root               | 27812 | filebeat      | 10.0.1.41        | 34062      | 10.0.1.43        | 9092        |
    | root               | 27812 | filebeat      | 10.0.1.41        | 49992      | 10.0.1.42        | 9092        |
    | root               | 27812 | filebeat      | 10.0.1.41        | 53158      | 10.0.1.41        | 9092        |
    | root               | 2796  | sssd_be       | 10.0.1.41        | 55640      | 10.0.1.89        | 389         |
    +--------------------+-------+---------------+------------------+------------+------------------+-------------+

Although osquery can write directly to Kafka, there's a [bug](https://github.com/osquery/osquery/issues/5890) when capturing deltas (full shapshots work just fine). As a workaround, I configured osquery to output to a file, and then used Elastic's FileBeat to tail the file and write the JSON to a Kafka topic.

osqueryd config:

    [root@cp01 ~]# cat /etc/osquery/osquery.conf 
    {
      "packs": {
        "system-snapshot": {
          "queries": {
            "processes_by_port": {
              "query": "select u.username, p.pid, p.name, pos.local_address, pos.local_port, pos.remote_address, pos.remote_port from processes p join users u on u.uid = p.uid join process_open_sockets pos on pos.pid=p.pid where pos.remote_port != '0';",
              "interval": 10,
              "snapshot": false,
              "removed": true
            }
          }
        }
      }
    }

FileBeat config:

    [root@cp01 ~]# cat /etc/filebeat/filebeat.yml 
    filebeat.inputs:
    
    - type: log
      enabled: true
      paths:
        - /var/log/osquery/osqueryd.results.log
    
    output.kafka:
      hosts: ["cp01.woolford.io:9092", "cp02.woolford.io:9092", "cp03.woolford.io:9092"]
    
      codec.format:
        string: '%{[message]}'
    
      topic: 'process-port'
      partition.round_robin:
        reachable_only: false
    
      required_acks: 1
      compression: gzip
      max_message_bytes: 1000000

This checks for changes and output them to the `process-ports` topic:

    {"name":"pack_system-snapshot_processes_by_port","hostIdentifier":"cp02.woolford.io","calendarTime":"Wed Oct 16 16:58:06 2019 UTC","unixTime":1571245086,"epoch":0,"counter":5629,"logNumericsAsNumbers":false,"columns":{"local_address":"10.0.1.42","local_port":"41560","name":"java","pid":"1755","remote_address":"10.0.1.43","remote_port":"9092","username":"cp-ksql"},"action":"removed"}
    {"name":"pack_system-snapshot_processes_by_port","hostIdentifier":"zk02.woolford.io","calendarTime":"Wed Oct 16 16:58:28 2019 UTC","unixTime":1571245108,"epoch":0,"counter":5550,"logNumericsAsNumbers":false,"columns":{"local_address":"10.0.1.32","local_port":"52544","name":"filebeat","pid":"29804","remote_address":"10.0.1.41","remote_port":"9092","username":"root"},"action":"added"}

The source code in this Kafka Streams app converts the messages above to a stream of events suitable for joining in a KTable or Kafka Streams table.

    key=10.0.1.42:41560:10.0.1.43:9092  value=null
    key=10.0.1.32:52544:10.0.1.41:9092  value={"name":"filebeat","pid":"29804",,"username":"root"}

Note that the "action" in the first record is a delete, and so the value is `null`. In the second record, the value is a JSON document containing the process name, process ID, and the username.