package net.floodlightcontroller.applications.fade.identifier;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.flow.Flow;

import java.util.List;

/**
 * Identify anomalous rules from flow
 */
public interface AnomalyIdentifier {
    /**
     * identify anomaly by a flow's rules.
     * Note, we assume there is only one anomaly in a flow
     * @param flow a constraint's suspicious flows
     * @return if the anomaly is identified, return the anomalous rule, otherwise return null
     */
    IRuleNode identifyAnomaly(List<Flow> flow);
}
