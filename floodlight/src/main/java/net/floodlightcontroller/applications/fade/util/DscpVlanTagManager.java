package net.floodlightcontroller.applications.fade.util;

import com.google.common.collect.Lists;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanPcp;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;

import java.util.List;

/**
 * a tag manager that leverage DSCP, Vlan vid, Vlan Pcp fields
 */
public class DscpVlanTagManager extends AbstractTagManager {
    private static final double DEFAULT_DETECTION_TAG_RATIO = .8;
    private static final int IP_DSCP_LEN = 6;
    private static final int VLAN_VID_LEN = 12;
    private static final int VLAN_PCP_LEN = 3;
    private static final int BLANK_TAG = 0;

    public DscpVlanTagManager(double ratioOfDetectionTag) {
        super(VLAN_PCP_LEN + IP_DSCP_LEN + VLAN_VID_LEN, ratioOfDetectionTag);
    }

    public DscpVlanTagManager(){
        this(DEFAULT_DETECTION_TAG_RATIO);
    }

    @Override
    public int requestDetectionTag() {
        // in order to distinct detection traffic from normal traffic,
        // the tag must have a non-zero ToS value
        int tag = super.requestDetectionTag();
        while(this.getDscp(tag) == 0){
            super.releaseTag(tag);
            tag = super.requestDetectionTag();
        }
        return tag;
    }

    @Override
    public int getBlankTag() {
        return BLANK_TAG;
    }

    @Override
    public void createTagMatch(int tag, Match.Builder mb) {
        int vlanVid = this.getVlanVid(tag);
        byte dscp = this.getDscp(tag);
        byte vlanPcp = this.getVlanPcp(tag);
        if(vlanVid != 0)
            mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlan(vlanVid));
        // always match ToS
        mb.setExact(MatchField.IP_DSCP, IpDscp.of(dscp));
        if(vlanPcp != 0)
            mb.setExact(MatchField.VLAN_PCP, VlanPcp.of(vlanPcp));
    }

    @Override
    public List<OFAction> createAttachTagAction(int tag, OFFactory ofFactory) {
        int vlanVid = this.getVlanVid(tag);
        byte dscp = this.getDscp(tag);
        byte vlanPcp = this.getVlanPcp(tag);
        List<OFAction> actions = Lists.newArrayList();
        actions.add(ofFactory.actions().buildSetVlanVid().setVlanVid(VlanVid.ofVlan(vlanVid)).build());
        actions.add(ofFactory.actions().buildSetNwTos().setNwTos((byte)(dscp << 2)).build());
        actions.add(ofFactory.actions().buildSetVlanPcp().setVlanPcp(VlanPcp.of(vlanPcp)).build());
        return actions;
    }

    @Override
    public List<OFAction> createStripTagAction(int tag, OFFactory ofFactory) {
        return Lists.newArrayList(
                ofFactory.actions().buildSetVlanVid().setVlanVid(VlanVid.ZERO).build(),
                ofFactory.actions().buildSetNwTos().setNwTos((byte)0).build(),
                ofFactory.actions().buildSetVlanPcp().setVlanPcp(VlanPcp.NONE).build()
        );
    }

    private byte getDscp(int tag){
        return (byte)((tag >> VLAN_VID_LEN) & 0x3F);
    }

    private int getVlanVid(int tag){
        return tag & 0xFFF;
    }

    private byte getVlanPcp(int tag){
        return (byte)((tag >> (VLAN_VID_LEN + IP_DSCP_LEN)) & 0x07);
    }
}
