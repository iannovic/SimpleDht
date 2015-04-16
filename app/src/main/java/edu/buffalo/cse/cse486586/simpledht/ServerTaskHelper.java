package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ianno_000 on 3/31/2015.
 */
public class ServerTaskHelper {

    private class Node {

        String port;
        boolean isInNetwork;
        String successor;
        String predecessor;

        public Node(String port) {
            this.port = port;
            isInNetwork = false;
            successor = "";
            predecessor = "";
        }

    }

    private ArrayList<Node> list;
    private int currentNodeCount = 1;

    public ServerTaskHelper() {
        list = new ArrayList<Node>();
        for (int i = 0; i < SimpleDhtActivity.MAX_NUMBER_OF_PROCESSES; i++) {
            String id = SimpleDhtActivity.EMULATOR_PORTS_ARRAY[i];
            Node node = new Node(id);
            if (id.equals("5554")) {
                node.isInNetwork = true;
            }
            list.add(node);
        }
       randomTest();
    }

    public void randomTest() {

        try {

            //placed into 5554
            //should be in 5556?
            String key = genHash("mPFPcloeSNcpDt6gFOs8KNUC8qNSRs6n");
            //5554 5556
            String node1 = genHash("5554");
            String node2 = genHash("5556");
            String node3 = genHash("5558");
            String node4 = genHash("5560");
            String node5 = genHash("5562");
            Log.i("RANDOM","1,2 1,3 1,4 1,5 " + node1.compareTo(node2) + " " + node1.compareTo(node3) + " " + node1.compareTo(node4) + " "+ node1.compareTo(node5));
            Log.i("RANDOM","2,1 3,1 4,1 5,1 " + node2.compareTo(node1) + " " + node3.compareTo(node1) + " " + node4.compareTo(node1) + " "+ node5.compareTo(node1));

            Log.i("RANDOM","key: " + key);
            Log.i("RANDOM","node1: " + node1);
            Log.i("RANDOM","node2:"  + node2);
            Log.i("RANDOM","key.compareTo(node1): " + key.compareTo(node1));
            Log.i("RANDOM","key.compareTo(node2): " + key.compareTo(node2));
            Log.i("RANDOM","node1.compareTo(node2): " + node1.compareTo(node2));

            Log.i("RANDOM","key.compareTo(node2): " + key.compareTo(node2));
            Log.i("RANDOM","key.compareTo(node1): " + key.compareTo(node1));
            Log.i("RANDOM","node2.compareTo(node1): " + node2.compareTo(node1));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }
    public void addNode(String port){
        for (int i = 0; i < list.size(); i ++) {
            Node node = list.get(i);

            if (node.port.equals(port)) {
                node.isInNetwork = true;
                currentNodeCount++;
                try {
                    SimpleDhtActivity.joinVariableSemaphore.acquire();
                    SimpleDhtActivity.CURRENT_NODE_COUNT = currentNodeCount;
                    SimpleDhtActivity.joinVariableSemaphore.release();
                } catch (InterruptedException e) {
                    Log.e("LOCK","",e);
                }

            }
        }
    }


    public List<ChordPojo> getNodeChanges() {
        Log.i("NODE_CHANGES","getNodeChanges() invoked");
        List<ChordPojo> updateList = new LinkedList<ChordPojo>();
        for (int i = 0; i < list.size(); i ++) {
            Node node = list.get(i);
            if (node.isInNetwork && didLinksChange(node.port)) {
                ChordPojo pojo = new ChordPojo();
                pojo.setType(ChordPojo.SUCCESSOR_PREDECESSOR_UPDATE);
                pojo.setSecond_type(ChordPojo.SUCCESSOR_PREDECESSOR_UPDATE);
                pojo.setDestinationId(node.port);
                pojo.setPredecessor(node.predecessor);
                pojo.setSuccessor(node.successor);
                pojo.setCurrentNodeCount(currentNodeCount);
                updateList.add(pojo);
            }
        }
        printLinks();
        return updateList;
    }
    public boolean didLinksChange(String port) {
        /*
            UPDATES THE LINKS WITH THE NEW TABLE FOR A GIVEN NODE AND DETERMINES WHETHER OR NOT WE NEED TO SEND UPDATE TO THE OTHER NODES
         */
        String predecessor = "";
        String successor = "";
        for (int i = 0; i < list.size(); i ++) {
            if (port.equals(list.get(i).port)) {
                predecessor = list.get(i).predecessor;
                successor = list.get(i).successor;
            }
        }
        Node node = determineNodeLinks(port);

        if (predecessor.equals(node.predecessor) && successor.equals(node.successor)) {
            Log.i("DETERMINE_LINKS",port + ": LINKS DID NOT CHANGE");
            return false;
        } else {
            Log.i("DETERMINE_LINKS",port + ": LINKS CHANGED");
            return true;

        }

    }

    private Node determineNodeLinks(String port) {
        Log.i("DETERMINE_LINKS", port + ":BEGINNING");
        Node retNode = new Node(port);
        String final_successor = "";
        String final_predecessor = "";

        try {
            String myNodeId = genHash(port);

//            /**************************************
//             * DETERMINE PREDECESSOR
//             *************************************/
//            String mostRecentNode = "";
//            int mostRecent = 100;
//            String greatestNode = "";
//            int greatestIndex = 100;
//
//
//
//            for (int i = 0; i < SimpleDhtActivity.MAX_NUMBER_OF_PROCESSES; i++) {
//                if (list.get(i).isInNetwork) {
//                    String tempPort = list.get(i).port;
//                    String tempNodeId = genHash(tempPort);
//
//                    if (mostRecentNode.equals("") && myNodeId.compareTo(tempNodeId) > 0) {
//                        mostRecentNode = tempNodeId;
//                        mostRecent = i;
//
//                    } else if (mostRecent != 100 && myNodeId.compareTo(tempNodeId) > 0 && tempNodeId.compareTo(mostRecentNode) > 0) {
//                        mostRecentNode = tempNodeId;
//                        mostRecent = i;
//
//                    }else if (greatestNode.equals("") && myNodeId.compareTo(tempNodeId) < 0) {
//                        greatestNode = tempNodeId;
//                        greatestIndex = i;
//                    } else if (greatestIndex != 100 && myNodeId.compareTo(tempNodeId) < 0 && tempNodeId.compareTo(greatestNode) > 0) {
//                        greatestNode = tempNodeId;
//                        greatestIndex = i;
//                    }
//                }
//            }
//            if (mostRecent == 100) {
//                final_predecessor = list.get(greatestIndex).port;
//            } else
//                final_predecessor = list.get(mostRecent).port;
//            /**************************************
//             * DETERMINE SUCCESSOR
//             *************************************/
//            mostRecentNode = "";
//            mostRecent = 100;
//            greatestNode = "";
//            greatestIndex = 100;
//
//            for (int i = 0; i < SimpleDhtActivity.MAX_NUMBER_OF_PROCESSES; i++) {
//                if (list.get(i).isInNetwork) {
//                    String tempPort = list.get(i).port;
//                    String tempNodeId = genHash(tempPort);
//
//                    if (mostRecentNode.equals("") && myNodeId.compareTo(tempNodeId) < 0) {
//                        mostRecentNode = tempNodeId;
//                        mostRecent = i;
//
//                    } else if (mostRecent != 100 && myNodeId.compareTo(tempNodeId) < 0 && tempNodeId.compareTo(mostRecentNode) > 0) {
//                        mostRecentNode = tempNodeId;
//                        mostRecent = i;
//
//                    }else if (greatestNode.equals("") && myNodeId.compareTo(tempNodeId) > 0) {
//                        greatestNode = tempNodeId;
//                        greatestIndex = i;
//                    } else if (greatestIndex != 100 && myNodeId.compareTo(tempNodeId) > 0 && tempNodeId.compareTo(greatestNode) < 0) {
//                        greatestNode = tempNodeId;
//                        greatestIndex = i;
//                    }
//                }
//            }
//            if (mostRecent == 100) {
//                final_successor = list.get(greatestIndex).port;
//            } else
//                final_successor = list.get(mostRecent).port;
//

            /**************************************
             *  OLD IMPLEMENTATION
             **************************************/
            String currentSuccessor = myNodeId;
            String currentPredecessor = myNodeId;
            int nearestBehind = 0;
            int furthestAhead = 0;
            int furthestAheadIndex = 0;

            int nearestAhead = 0;
            int furthestBehind = 0;
            int furthestBehindIndex = 0;

            for (int i = 0; i < SimpleDhtActivity.MAX_NUMBER_OF_PROCESSES; i++) {
                if (list.get(i).isInNetwork) {
                    String tempPort = list.get(i).port;
                    String tempNode = genHash(tempPort);

                    //Log.i(TAG,tempNode + ":" + myNodeId + ":" + currentPredecessor + ":" + currentSuccessor);
                    int distance = tempNode.compareTo(myNodeId);
                    Log.i("DETERMINE_LINKS", tempPort + ":" + distance);
                    if (nearestAhead > 0 && distance > 0 && distance < nearestAhead) {
                        nearestAhead = distance;
                        Log.i("DETERMINE_LINKS", tempPort + ":" + distance + ": SET SUCCESSOR");
                        final_successor = tempPort;

                    } else if (nearestAhead == 0 && distance > 0) {
                        nearestAhead = distance;
                        final_successor = tempPort;
                    }

                    if (furthestBehind > distance) {
                        furthestBehind = distance;
                        furthestBehindIndex = i;
                    }

                    if (nearestBehind < 0 && distance < 0 && distance > nearestBehind) {
                        nearestBehind = distance;
                        final_predecessor = tempPort;
                        Log.i("DETERMINE_LINK", tempPort + ":" + distance + ": SET PREDECESSOR");
                    } else if (nearestBehind == 0 && distance < 0) {
                        nearestBehind = distance;
                        final_predecessor = tempPort;
                    }

                    if (furthestAhead < distance) {
                        furthestAhead = distance;
                        furthestAheadIndex = i;
                    }
                }
            }
            if (final_successor.equals("")) {
                final_successor = list.get(furthestBehindIndex).port;
                Log.i("DETERMINE_LINK", final_successor + ":SUCCESSOR");
            }
            if (final_predecessor.equals("")) {
                final_predecessor = list.get(furthestAheadIndex).port;
                Log.i("DETERMINE_LINK", final_predecessor + ":PREDECESSOR");
            }

            /*
              CORNER CASE TO HANDLE WHEN THE NODE NETWORK IS OF SIZE 2
             */
            if (final_successor.equals("")) {
                final_successor = final_predecessor;
            }
            if (final_predecessor.equals("")) {
                final_predecessor = final_successor;
            }
        }catch (NoSuchAlgorithmException e) {
            Log.e("DETERMINE_LINK", "Failed to call genHash while initializing node", e);
        } finally {
            Log.i("DETERMINE_LINK", "emulator port is: " + port + " and predecessor port is: " + final_predecessor + " and successor port is: " + final_successor);
        }

        for (int i = 0; i < list.size(); i++) {
            if (port.equals(list.get(i).port)) {
                list.get(i).predecessor = final_predecessor;
                list.get(i).successor = final_successor;
            }
        }
        retNode.predecessor = final_predecessor;
        retNode.successor = final_successor;
        return retNode;
    }


    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public void printLinks() {
        Log.i("PRINTLINK","====================" + currentNodeCount + "================================");
        for (int i = 0; i < list.size(); i ++) {
            Node n = list.get(i);
            Log.i("PRINTLINK","Port:" + n.port + " successor:" + n.successor + " predecessor:" + n.predecessor + " inNetwork:" + n.isInNetwork );
        }
        Log.i("PRINTLINK","====================================================");
    }
}
