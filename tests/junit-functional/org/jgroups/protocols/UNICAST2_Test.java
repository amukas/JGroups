package org.jgroups.protocols;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.util.MyReceiver;
import org.jgroups.util.Util;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests UNICAST2. Created to test the last-message-dropped problem, see https://issues.jboss.org/browse/JGRP-1548.
 * @author Bela Ban
 * @since  3.3
 */
@Test(groups=Global.FUNCTIONAL,sequential=true)
public class UNICAST2_Test {
    protected JChannel            a, b;
    protected MyReceiver<Integer> rb=new MyReceiver<Integer>().name("B").verbose(true);
    protected DISCARD             discard; // on A

    @BeforeMethod protected void setup() throws Exception {
        a=createChannel("A");
        discard=(DISCARD)a.getProtocolStack().findProtocol(DISCARD.class);
        assert discard != null;
        a.connect("UNICAST2_Test");
        b=createChannel("B");
        b.setReceiver(rb);
        b.connect("UNICAST2_Test");
        Util.waitUntilAllChannelsHaveSameSize(10000, 500, a, b);
    }

    @AfterMethod protected void destroy() {Util.close(b, a);}


    /**
     * A sends unicast messages 1-5 to B, but we drop message 5. The code in
     * https://issues.jboss.org/browse/JGRP-1548 now needs to make sure message 5 is retransmitted to B
     * within a short time period, and we don't have to rely on the stable task to kick in.
     */
    public void testLastMessageDropped() throws Exception {
        Address dest=b.getAddress();
        for(int i=1; i <= 5; i++) {
            Message msg=new Message(dest, i);
            if(i == 5)
                discard.setDropDownUnicasts(1); // drops the next unicast
            a.send(msg);
        }

        List<Integer> msgs=rb.list();
        Util.waitUntilListHasSize(msgs, 5, 5000, 500);
        System.out.println("list=" + msgs);
    }

    /**
     * A sends unicast message 1 to B, but we drop message 1. The code in
     * https://issues.jboss.org/browse/JGRP-1563 now needs to make sure message 1 is retransmitted to B
     * within a short time period, and we don't have to rely on the stable task to kick in.
     */
    // @Test(invocationCount=10,threadPoolSize=0)
    public void testFirstMessageDropped() throws Exception {
        Address dest=b.getAddress();

        System.out.println("**** closing all connections ****");
        // close all connections, so we can start from scratch and send message A1 to B
        for(JChannel ch: Arrays.asList(a,b)) {
            UNICAST2 unicast=(UNICAST2)ch.getProtocolStack().findProtocol(UNICAST2.class);
            unicast.removeAllConnections();
        }

        setLevel("trace", a, b);

        System.out.println("--> A sending first message to B (dropped before it reaches B)");
        discard.setDropDownUnicasts(1); // drops the next unicast
        a.send(new Message(dest,1));

        List<Integer> msgs=rb.list();
        Util.waitUntilListHasSize(msgs, 1, 5000, 500);
        System.out.println("list=" + msgs);
        setLevel("warn", a, b);
    }


    protected JChannel createChannel(String name) throws Exception {
        JChannel channel=new JChannel(new SHARED_LOOPBACK(),
                                      new PING().setValue("timeout", 500),
                                      new NAKACK2().setValue("use_mcast_xmit", false),
                                      new DISCARD(),
                                      new UNICAST2().setValue("xmit_interval", 500),
                                      new GMS().setValue("print_local_addr", false));
        channel.setName(name);
        return channel;
    }

    protected void setLevel(String level, JChannel ... channels) {
        for(JChannel ch: channels)
            ch.getProtocolStack().findProtocol(UNICAST2.class).level(level);
    }
}