package org.jgroups.demos;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.util.Util;

import java.net.InetAddress;

/**
 * @author Bela Ban
 */
public class ProgrammaticChat {

    public static void main(String[] args) throws Exception {
        JChannel ch=new JChannel(new UDP().setValue("bind_addr", InetAddress.getByName("192.168.1.5")),
                                 new PING(),
                                 new MERGE2(),
                                 new FD_SOCK(),
                                 new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000),
                                 new VERIFY_SUSPECT(),
                                 new BARRIER(),
                                 new NAKACK2(),
                                 new UNICAST3(),
                                 new STABLE(),
                                 new GMS(),
                                 new UFC(),
                                 new MFC(),
                                 new FRAG2());

        ch.setReceiver(new ReceiverAdapter() {
            public void viewAccepted(View new_view) {
                System.out.println("view: " + new_view);
            }

            public void receive(Message msg) {
                System.out.println("<< " + msg.getObject() + " [" + msg.getSrc() + "]");
            }
        });

        ch.connect("ChatCluster");


        for(;;) {
            String line=Util.readStringFromStdin(": ");
            ch.send(null, line);
        }
    }

}


