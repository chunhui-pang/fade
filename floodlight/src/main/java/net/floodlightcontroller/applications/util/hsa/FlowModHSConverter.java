package net.floodlightcontroller.applications.util.hsa;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetVlanVid;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.python.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Our implementation of converting flow rule into headerspace.
 * In our implementation, we only consider VLAN, IPv4 destination.
 * Thus, a flow rule is converted into a ternary array which matches the VLAN and IPv4 destination address.
 *
 * @author chunhui (chunhui.pang@outlook.com)
 */
public class FlowModHSConverter implements IHSConverter<OFFlowMod> {
    private static final Logger logger = LoggerFactory.getLogger(FlowModHSConverter.class);
    private static final int ARRAY_LENGTH = 44;
    private static final int VLAN_VID_LENGTH = 12;
    private static final int IPV4_DST_LENGTH = 32;
    private static final int VLAN_VID_OFFSET = 0;
    private static final int IPV4_DST_OFFSET = 12;

    /**
     * convert a flow mod rule into header space
     * @implNote we return an match-all ternary array for {@code null} argument
     * @param obj the object to be converted
     * @return the ternary array converted from current flow rule
     */
    @Override
    public TernaryArray parse(OFFlowMod obj) {
        TernaryArray at = TernaryArray.getAllX(ARRAY_LENGTH);
        if(obj == null){
            return at;
        }
        OFVlanVidMatch vlanVid = obj.getMatch().get(MatchField.VLAN_VID);
        if(vlanVid == null){
            Masked<OFVlanVidMatch> maskedVlanVid = obj.getMatch().getMasked(MatchField.VLAN_VID);
            if(maskedVlanVid != null) {
                vlanVid = maskedVlanVid.getValue();
            }
        }
        if(vlanVid == null){
            for(int i = VLAN_VID_OFFSET; i < VLAN_VID_OFFSET + VLAN_VID_LENGTH; i++){
                at.setBit(i, 'x');
            }
        }else{
            short vid = vlanVid.getRawVid();
            for(int i = VLAN_VID_OFFSET; i < VLAN_VID_OFFSET + VLAN_VID_LENGTH; i++) {
                at.setBit(i, (vid & (0x01 << (i-VLAN_VID_OFFSET))) == 0 ? '0' : '1');
            }
        }

        Masked<IPv4Address> ipv4Dst = obj.getMatch().getMasked(MatchField.IPV4_DST);
        if(ipv4Dst == null){
            ipv4Dst = Masked.of(obj.getMatch().get(MatchField.IPV4_DST), IPv4Address.NO_MASK);
        }
        int mask = ipv4Dst == null ? 0 : ipv4Dst.getMask().getInt();
        if(ipv4Dst != null && Integer.bitCount(~mask) == 0){
            int value = ipv4Dst.getValue().getInt();
            for(int i = IPV4_DST_OFFSET; i < IPV4_DST_OFFSET + IPV4_DST_LENGTH; i++){
                at.setBit(i, (value & (0x01 << (i-IPV4_DST_OFFSET))) == 0 ? '0' : '1');
            }
        }else{
            if(ipv4Dst == null){
                for(int i = IPV4_DST_OFFSET; i < IPV4_DST_OFFSET + IPV4_DST_LENGTH; i++)
                    at.setBit(i, 'x');
            }else{
                int value =ipv4Dst.getValue().getInt();
                for(int i = IPV4_DST_OFFSET; i < IPV4_DST_OFFSET + IPV4_DST_LENGTH; i++){
                    at.setBit(i,
                            (mask & (0x01 << (i-IPV4_DST_OFFSET))) == 0 ?
                                    'x' :
                                    (value & (0x01 << (i-IPV4_DST_OFFSET))) == 0 ? '0' : '1');
                }
            }
        }
        return at;
    }

    @Override
    public Pair<TernaryArray, TernaryArray> parseRewriter(OFFlowMod obj) {
        if(obj == null){
            return null;
        }
        OFVlanVidMatch vlanVid = null;
        IPv4Address ipv4Dst = null;
        if(obj.getActions() == null)
            return null;
        for(OFAction action : obj.getActions()){
            switch(action.getType()){
                case OUTPUT:
                    // OUTPUT is handled by class FlowRule
                    break;
                case SET_VLAN_VID:
                    OFActionSetVlanVid setVid = (OFActionSetVlanVid) action;
                    vlanVid = OFVlanVidMatch.ofVlan(setVid.getVlanVid().getVlan());
                    break;
                case SET_NW_DST:
                    OFActionSetNwDst setNwDst = (OFActionSetNwDst) action;
                    ipv4Dst = setNwDst.getNwAddr();
                    break;
                default:
                    logger.warn("unsupported action with type {}, ignoring it!", action.getType());
                    break;
            }
        }
        if(vlanVid == null && ipv4Dst == null)
            return null;
        TernaryArray mask = TernaryArray.getAllOne(ARRAY_LENGTH);
        TernaryArray rewrite = TernaryArray.getAllZero(ARRAY_LENGTH);
        if(vlanVid != null){
            short id = vlanVid.getRawVid();
            for(int i = VLAN_VID_OFFSET; i < VLAN_VID_OFFSET + VLAN_VID_LENGTH; i++){
                mask.setBit(i, '0');
                rewrite.setBit(i, ((id >> (i-VLAN_VID_OFFSET)) & 0x01) == 0 ? '0' : '1');
            }
        }
        if(ipv4Dst != null){
            int id = ipv4Dst.getInt();
            for(int i = IPV4_DST_OFFSET; i < IPV4_DST_OFFSET + IPV4_DST_LENGTH; i++){
                mask.setBit(i, '0');
                rewrite.setBit(i, ((id >> (i-IPV4_DST_OFFSET)) & 0x01) == 0 ? '0' : '1');
            }
        }
        return Pair.of(mask, rewrite);
    }

    @Override
    public HeaderSpace parseHeaderSpace(OFFlowMod obj) {
        HeaderSpace hs = new HeaderSpace(ARRAY_LENGTH);
        hs.appendArray(this.parse(obj));
        return hs;
    }

    @Override
    public OFFlowMod read(TernaryArray array, OFFactory ofFactory) {
        OFFlowMod.Builder fmb = ofFactory.buildFlowAdd();
        Match.Builder mb = ofFactory.buildMatch();
        mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
        int ipv4Dst = 0, ipv4DstMask = 0;
        for(int i = IPV4_DST_OFFSET; i < IPV4_DST_OFFSET + IPV4_DST_LENGTH; i++){
            Pair<Integer, Integer> pair = this.setBitWithMask(ipv4Dst, ipv4DstMask, i - IPV4_DST_OFFSET, array.getBit(i));
            ipv4Dst = pair.getLeft();
            ipv4DstMask = pair.getRight();
        }
        if(Integer.bitCount(ipv4DstMask) == IPV4_DST_LENGTH){
            mb.setExact(MatchField.IPV4_DST, IPv4Address.of(ipv4Dst));
        } else {
            mb.setMasked(MatchField.IPV4_DST, IPv4Address.of(ipv4Dst), IPv4Address.of(ipv4DstMask));
        }

        int vlanVid = 0, vlanVidMask = 0;
        for(int i = VLAN_VID_OFFSET; i < VLAN_VID_OFFSET + VLAN_VID_LENGTH; i++){
            Pair<Integer, Integer> pair = this.setBitWithMask(vlanVid, vlanVidMask, i - VLAN_VID_OFFSET, array.getBit(i));
            vlanVid = pair.getLeft();
            vlanVidMask = pair.getRight();
        }
        if(Integer.bitCount(vlanVidMask) == VLAN_VID_LENGTH){
            mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlan(vlanVid));
        } else if(vlanVid != 0 || vlanVidMask != 0){
            mb.setMasked(MatchField.VLAN_VID, OFVlanVidMatch.ofVlan(vlanVid), OFVlanVidMatch.ofVlan(vlanVidMask));
        }
        fmb.setMatch(mb.build());
        return fmb.build();
    }

    @Override
    public List<OFFlowMod> read(HeaderSpace hs, OFFactory ofFactory) {
        hs.cleanup();
        List<OFFlowMod> result = Lists.newArrayListWithCapacity(hs.getAdditions().size());
        for(TernaryArray array : hs.getAdditions()){
            result.add(this.read(array, ofFactory));
        }
        return result;
    }

    /**
     * bit pos starts from 0, and start with the least significant bit.
     */
    private int setBit(int val, int bitPos, int bitVal) {
        if (0 == bitVal) {
            int mask = (1 << bitPos);
            val = (val & (~mask));
        } else {
            int mask = ( 1 << bitPos);
            val = (val | mask);
        }
        return val;
    }

    private Pair<Integer, Integer> setBitWithMask(int val, int mask, int bitPos, char bitVal){
        switch(bitVal){
            case '0':
                val = this.setBit(val, bitPos, 0);
                mask = this.setBit(mask, bitPos, 1);
                break;
            case '1':
                val = this.setBit(val, bitPos, 1);
                mask = this.setBit(mask, bitPos, 1);
                break;
            case 'x':
                val = this.setBit(val, bitPos, 0);
                mask = this.setBit(mask, bitPos, 0);
                break;
            default:
                break;
        }
        return new ImmutablePair<>(val, mask);
    }
}
