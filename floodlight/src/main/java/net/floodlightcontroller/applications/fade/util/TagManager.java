package net.floodlightcontroller.applications.fade.util;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;

import java.util.List;

/**
 * Manage all tags.
 * Tag is used to embed in dedicated rules so that all dedicated rules generated for the same flow would count the same set of packets.
 * In general, the first dedicated rule attach a tag to every packets it counts,
 * and the other dedicated rules would match on this tag.
 *
 * Besides, we have another set of tag which would be attached to packets which have not been detected.
 * These tags are randomly generated, and is used to confuse attackers.
 */
public interface TagManager {
    /**
     * request for a new tag for dedicated rules
     * @return the new tag for dedicated rules
     */
    int requestDetectionTag();

    /**
     * get the number of available tags
     * @return the number of available tags
     */
    int getNumOfAvailableTag();

    /**
     * request for a new tag for general packets (not been detected)
     * @return the new tag for confusing attackers
     */
    int requestConfusingTag();

    /**
     * request for the blank tag that match all untagged packets (or normal packets)
     * @return the blank tag
     */
    int getBlankTag();

    /**
     * release a used tag.
     * This function should be called when the flow that uses this tag expires.
     * @param tag the tag that used by a expired flow
     */
    void releaseTag(int tag);

    /**
     * create the related match for the tag
     * @param tag the tag
     * @param mb an existed match builder
     */
    void createTagMatch(int tag, Match.Builder mb);

    /**
     * create actions for the tag
     * @param tag the tag
     * @param ofFactory the openflow factory
     * @return the actions (unmodifiable)
     */
    List<OFAction> createAttachTagAction(int tag, OFFactory ofFactory);

    /**
     * create the actions to strip the tag
     * @param tag the tag
     * @param ofFactory the openflow factory
     * @return the actions (unmodifiable)
     */
    List<OFAction> createStripTagAction(int tag, OFFactory ofFactory);
}
