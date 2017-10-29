package net.floodlightcontroller.applications.fade.constraint.postvalidateaction;

import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.util.TagManager;

/**
 * Releasing tag
 */
public class TagReleasePostValidateAction implements PostValidateAction {
    private TagManager tagManager;
    private int tag;

    public TagReleasePostValidateAction(TagManager tagManager, int tag){
        this.tagManager = tagManager;
        this.tag = tag;
    }

    @Override
    public void execute(Constraint constraint, boolean validationResult) {
        this.tagManager.releaseTag(tag);
    }

    @Override
    public String toString() {
        return "TagReleasePostValidateAction{" +
                "tag=" + tag +
                '}';
    }
}
