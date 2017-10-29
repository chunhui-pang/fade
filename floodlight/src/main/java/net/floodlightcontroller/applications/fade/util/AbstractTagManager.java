package net.floodlightcontroller.applications.fade.util;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link TagManager}.
 * We use TOS (6bit) or VLAN_ID (12bit) as field
 */
public abstract class AbstractTagManager implements TagManager {
    private static final Logger logger = LoggerFactory.getLogger(AbstractTagManager.class);

    private static final int START_OF_TAG = 1;
    private static final double DEFAULT_RATIO_OF_DETECTION_TAG = 0.8;

    private int maxTag;
    private double ratioOfDetectionTag;

    private BitSet detectingTag;
    private BitSet usedDetectingTag;
    private ReentrantLock detectingTagLock;
    private ArrayList<Integer> confusingTag;

    private Random random;


    public AbstractTagManager(int numOfBits, double ratioOfDetectionTag){
        this.maxTag = (1 << numOfBits);
        this.ratioOfDetectionTag = ratioOfDetectionTag;
        this.detectingTag = new BitSet(this.maxTag);
        this.detectingTagLock = new ReentrantLock();
        this.usedDetectingTag = new BitSet(this.maxTag);
        this.random = new SecureRandom();

        for(int tag = START_OF_TAG; tag < this.maxTag; tag++){
            if(random.nextFloat() < this.ratioOfDetectionTag){
                this.detectingTag.set(tag);
            }
        }
        // Optimization: the confusing tag is strictly an array
        this.confusingTag = Lists.newArrayListWithCapacity(this.maxTag - this.detectingTag.cardinality());
        for(int i = 0; i < this.maxTag; i++){
            if(!this.detectingTag.get(i)){
                this.confusingTag.add(i);
            }
        }
    }


    @Override
    public int requestDetectionTag() {
        int tag;
        this.detectingTagLock.lock();
        try {
            // generate a random number from 0 - numberof(setted bit)
            int numberOfSetBit = this.detectingTag.cardinality();
            if(numberOfSetBit == 0){
                throw new TagRunOutException("detecting tags are run out, please use more bits.");
            }
            int idx = random.nextInt(numberOfSetBit) + 1;
            if (idx < numberOfSetBit / 2) {
                tag = this.detectingTag.nextSetBit(0);
                while (tag >= 0 && (--idx) != 0) {
                    tag = this.detectingTag.nextSetBit(tag + 1);
                }
            } else {
                idx = numberOfSetBit - idx + 1;
                tag = this.detectingTag.previousSetBit(this.detectingTag.length() - 1);
                while (tag >= 0 && (--idx) != 0) {
                    tag = this.detectingTag.previousSetBit(tag - 1);
                }
            }
            this.detectingTag.clear(tag);
            this.usedDetectingTag.set(tag);
        } finally {
            this.detectingTagLock.unlock();
        }
        return tag;
    }

    @Override
    public int getNumOfAvailableTag() {
        this.detectingTagLock.lock();
        try {
            return this.detectingTag.cardinality();
        } finally {
            this.detectingTagLock.unlock();
        }
    }

    @Override
    public int requestConfusingTag() {
        int idx = this.random.nextInt(this.confusingTag.size());
        return this.confusingTag.get(idx);
    }

    @Override
    public void releaseTag(int tag) {
        this.detectingTagLock.lock();
        try {
            if (this.usedDetectingTag.get(tag)) {
                this.detectingTag.flip(tag);
                this.usedDetectingTag.flip(tag);
                return;
            }
        } finally {
            this.detectingTagLock.unlock();
        }
        logger.error("cannot release tag, the tag {} hasn't been used yet.", tag);
    }
}
