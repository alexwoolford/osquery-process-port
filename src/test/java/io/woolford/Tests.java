package io.woolford;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class Tests {

    @Test
    public void TwoDifferentRecords(){

        Set<ProcessPortRecord> processPortRecordSet = new HashSet<ProcessPortRecord>();;

        ProcessPortRecord processPortRecord = new ProcessPortRecord();
        processPortRecord.setLocalAddress("10.0.1.1");
        processPortRecord.setLocalPort("1234");
        processPortRecord.setName("java");
        processPortRecord.setPid("3456");
        processPortRecord.setRemoteAddress("10.0.1.10");
        processPortRecord.setRemotePort("9876");
        processPortRecord.setUsername("cp-kafka");

        processPortRecordSet.add(processPortRecord);

        ProcessPortRecord processPortNewRecord = new ProcessPortRecord();
        processPortNewRecord.setLocalAddress("10.0.1.1");
        processPortNewRecord.setLocalPort("1234");
        processPortNewRecord.setName("osquery");
        processPortNewRecord.setPid("3456");
        processPortNewRecord.setRemoteAddress("10.0.1.10");
        processPortNewRecord.setRemotePort("9876");
        processPortNewRecord.setUsername("cp-kafka");

        processPortRecordSet.add(processPortNewRecord);

        assert processPortRecordSet.size() == 2;

    }

    @Test
    public void TwoIdenticalRecords(){

        Set<ProcessPortRecord> processPortRecordSet1 = new HashSet<ProcessPortRecord>();;

        ProcessPortRecord processPortRecord = new ProcessPortRecord();
        processPortRecord.setLocalAddress("10.0.1.1");
        processPortRecord.setLocalPort("1234");
        processPortRecord.setName("java");
        processPortRecord.setPid("3456");
        processPortRecord.setRemoteAddress("10.0.1.10");
        processPortRecord.setRemotePort("9876");
        processPortRecord.setUsername("cp-kafka");

        processPortRecordSet1.add(processPortRecord);


        Set<ProcessPortRecord> processPortRecordSet2 = new HashSet<ProcessPortRecord>();;

        ProcessPortRecord processPortNewRecord = new ProcessPortRecord();
        processPortRecord.setLocalAddress("10.0.1.1");
        processPortRecord.setLocalPort("1234");
        processPortRecord.setName("java");
        processPortRecord.setPid("3456");
        processPortRecord.setRemoteAddress("10.0.1.10");
        processPortRecord.setRemotePort("9876");
        processPortRecord.setUsername("cp-kafka");

        processPortRecordSet2.add(processPortNewRecord);


        System.out.println("OK");

    }



    //            String localAddress;
    //            String localPort;
    //            String name;
    //            String pid;
    //            String remoteAddress;
    //            String remotePort;
    //            String username;




}
