import javafx.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by lightray on 4/8/17.
 */
public class BTree implements Serializable{
    int largestKey;
    int smallestKey;

    //t
    int minDegree;

    //T.root
    Node root;

    //2t - 1
    int maxDegree;

    File fileOnDisk = new File("values.data");
    /*
        Notes on the RAF:
        long at position 0: indicates the location of the seeker
            seeker indicates next location in the RAF
            where a node can be written
        Nodes
            x bytes long
            position in file indicated by Node.spotInFile field
     */
    RandomAccessFile RAF;

    File serializedNodes = new File("nodes.bstrc");

    long pointer;

    ArrayList<Integer> allKeys = new ArrayList<Integer>();
    ArrayList<String> allKeyNames = new ArrayList<String>();


    public BTree(int minDegree) throws IOException {
        maxDegree = (2 * minDegree) - 1;
        System.out.println("MAXDEGREE: " + maxDegree);
        this.minDegree = minDegree;
        System.out.println("MINDEGREE: " + minDegree);
        pointer = 0;

        largestKey = 0;
        smallestKey = 0;

//        if(serializedNodes.exists()){
//            try {
////                RAF = new RandomAccessFile(fileOnDisk, "rw");
////                RAF.seek(0);
////                pointer = RAF.readLong();
//            } catch (Exception e){
//                e.printStackTrace();
//            }
//
//            root = DISK_READ(0);
//
//        } else {

            try {
//                RAF = new RandomAccessFile(fileOnDisk, "rw");
//                RAF.seek(0);
//                RAF.writeLong(0);
                Node x = ALLOCATE_NODE();
                x.leaf = true;
                x.count = 0;
                DISK_WRITE(x);
                root = x;
            } catch (Exception e){
                e.printStackTrace();
            }
        //}

    }


    void splitChild(Node x, int i){
       // System.out.println("CALLING SPLIT");
        Node z = ALLOCATE_NODE();
        Node y = x.children[i];
        z.leaf = y.leaf;
        z.count = minDegree - 1;

        for(int j = 1; j < minDegree; j++){
            z.keys[j] = y.keys[j+minDegree];
            z.data.put(y.keys[j+minDegree], y.data.get(y.keys[j+minDegree]));
        }

        if(!y.leaf){
            for(int j = 1; j <= minDegree; j++){
                z.children[j] = y.children[j+minDegree];
            }
        }

        y.count = minDegree - 1;
        for(int j = x.count + 1; j >= i + 1; j--){
            x.children[j+1] = x.children[j];
        }
        x.children[i+1] = z;

        for(int j = x.count; j >= i; j--){
            x.keys[j+1] = x.keys[j];
        }
        x.keys[i] = y.keys[minDegree];
        x.data.put(x.keys[i], y.data.get(y.keys[minDegree]));
        x.count = x.count+1;

        DISK_WRITE(y);
        DISK_WRITE(z);
        DISK_WRITE(x);
    }

    public void insert(BTree t, WeatherData data){
        int k = data.k;

        //no duplicates
        if(!allKeys.contains(k)) {
            System.out.println("CALLING INSERT FOR " + data.k);

            allKeyNames.add(data.cityName);
            allKeys.add(k);

           // System.out.println("Adding " + data + " to tree with key " + k);

            if (k < smallestKey) {
                smallestKey = k;
            } else if (k > largestKey) {
                largestKey = k;
            }


            Node r = t.root;
            if (r.count == maxDegree) {
                Node s = ALLOCATE_NODE();
                t.root = s;
                s.leaf = false;
                s.count = 0;
                s.children[1] = r;
                splitChild(s, 1);
                s = insert_nonfull(s, data);

            } else {
                //System.out.println("ROOT'S COUNT " + root.count);
                r = insert_nonfull(r, data);
            }
        }
    }

    private Node insert_nonfull(Node x, WeatherData data) {
       // System.out.println("CALLING INSERT NONFULL");
        int k = data.k;
        int i = x.count;
        if(i == 7){
            //attach to this line
            //System.out.println("MEOW!!");
        }
        if(x.leaf){
            //System.out.println("I: " + i);
            //System.out.println(x.keys[i]);
            while(i >= 1 && k < x.keys[i]){
                x.keys[i+1] = x.keys[i];
                i = i - 1;
               // System.out.println("I: " + i);
            }
            x.keys[i+1] = k;
            x.data.put(k, data);
            x.count++;
            DISK_WRITE(x);
        } else {
            while(i >= 1 && k < x.keys[i]){
                i--;
                //System.out.println(x.keys[i]);
            }
            i++;
            //System.out.println("GETTING CHILD AT " + i);
            Node child = DISK_READ(x.children[i].spotInFile);
            if(child.count == maxDegree){
                splitChild(x,i);
                if(k > x.keys[i]){
                    i++;
                }
            }
            child = DISK_READ(x.children[i].spotInFile);
            x.children[i] = insert_nonfull(child,data);
            DISK_WRITE(x.children[i]);
        }
        return x;
    }


    public Pair<Node,Integer> search(Node x, int key){
       // System.out.println("CALLING SEARCH");
        int i = 1;
        while(i <= x.count && key > x.keys[i]){
            i++;
        }

        if(i <= x.count && key == x.keys[i]){

            return(new Pair<Node,Integer>(x,i));

        } else if(x.leaf){
            return null;
        } else {
            Node child = DISK_READ(x.children[i].spotInFile);
            return search(child, key);
        }
    }

    public WeatherData get(int key){
        Pair<Node,Integer> nodeIntegerPair = search(root,key);

        Node n = nodeIntegerPair.getKey();
        return n.getData(key);
    }

    private Node DISK_READ(long position) {
       // System.out.println("CALLING DISK READ");
        if(serializedNodes.exists()){
            ObjectInputStream ois;
            try{
                ois = new ObjectInputStream(new FileInputStream(serializedNodes.getName()));
                Integer length = ois.readInt();
                for(int i = 0; i < length; i++){
                    long key = ois.readLong();
                    Node n = (Node) ois.readObject();
                    if(key == position){
                        return n;
                    }
                }
                ois.close();

            }catch(Exception e){
                System.out.println(e);
            }
        }
        return null;
    }

    private Node ALLOCATE_NODE() {
        Node x = new Node(minDegree);
        return x;
    }

    private void DISK_WRITE(Node x) {
       // System.out.println(x.spotInFile);
       // System.out.println("CALLING DISK WRITE");
        if(serializedNodes.exists()){
            HashMap<Long, Node> lines = new HashMap<Long,Node>();
            ObjectInputStream ois = null;
            try{


                boolean found = false;
                ois = new ObjectInputStream(new FileInputStream(serializedNodes.getName()));
                Integer length = ois.readInt();
                for(int i = 0; i < length; i++){
                    long key = ois.readLong();
                    Node n;
                    if(key == x.spotInFile){
                        n = x;
                        ois.readObject();
                        found = true;
                    } else {
                        n = (Node) ois.readObject();
                    }
                    lines.put(key,n);
                }
                ois.close();


                FileOutputStream fos = new FileOutputStream(serializedNodes.getName());
                ObjectOutputStream oos = new ObjectOutputStream(fos);

                if(!found) {
                    length = length + 1;
                }
                oos.writeInt(length);
                long lastKey = 0;
                for(Long key : lines.keySet()){
                    oos.writeLong(key);
                   // if(lines.get(key) == x)
                    oos.writeObject(lines.get(key));
                    lastKey = key;
                }

                if(!found) {
                    long writeKey = lastKey + 1;
                    x.spotInFile = writeKey;
                    oos.writeLong(writeKey);
                    oos.writeObject(x);
                }



            }catch(Exception e){
                System.out.println(e);
            }
        } else {

            try{
                FileOutputStream fos = new FileOutputStream(serializedNodes.getName());
                ObjectOutputStream oos = new ObjectOutputStream(fos);

                oos.writeInt(1);
                x.spotInFile = 0;
                oos.writeLong(0);
                oos.writeObject(x);

            } catch(Exception e){
                System.out.println(e);
            }

        }
    }





}
