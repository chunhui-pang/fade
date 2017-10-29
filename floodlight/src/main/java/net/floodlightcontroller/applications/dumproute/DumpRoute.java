package net.floodlightcontroller.applications.dumproute;

import java.util.*;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;

import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DumpRoute implements IFloodlightModule, IDumpRouteService {
	private static final Logger logger = LoggerFactory.getLogger(DumpRoute.class);
	private IRoutingService routingService;
	private IDeviceService deviceService;
	private IRestApiService restApiService;
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return Collections.singleton(IDumpRouteService.class);
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return Collections.singletonMap(IDumpRouteService.class, this);
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        return Arrays.asList(IRoutingService.class, IDeviceService.class, IRestApiService.class);
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		this.routingService = context.getServiceImpl(IRoutingService.class);
		this.deviceService = context.getServiceImpl(IDeviceService.class);
		this.restApiService = context.getServiceImpl(IRestApiService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		this.restApiService.addRestletRoutable(new DumpRouteRoutable());
	}

	@Override
	public Route getRoute(MacAddress srcMac, MacAddress dstMac) {
		IDevice srcDev,	dstDev;
		Iterator<? extends IDevice> srcTmp = this.deviceService.queryDevices(srcMac, null, IPv4Address.NONE, IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);
        Iterator<? extends IDevice> dstTmp = this.deviceService.queryDevices(dstMac, null, IPv4Address.NONE, IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);
        if(!srcTmp.hasNext() || !dstTmp.hasNext()){
            logger.error("cannot find device with MAC address set to {}", srcTmp.hasNext() ? dstMac : srcMac);
            return null;
        } else {
			srcDev = srcTmp.next();
            dstDev = dstTmp.next();
			if(srcTmp.hasNext()  || dstTmp.hasNext()){
				logger.error("find multiple device with MAC set to {}", srcTmp.hasNext() ? srcMac : dstMac);
				return null;
			}
		}

		SwitchPort[] srcSp = srcDev.getAttachmentPoints(),
				dstSp = dstDev.getAttachmentPoints();
		if(srcSp.length != 1){
			logger.error("find multiple attachment points {} for device {}", srcSp, srcMac);
		}
		if(dstSp.length != 1){
			logger.error("find multiple attachment points {} for device {}", dstSp, dstMac);
		}
		return this.routingService.getRoute(srcSp[0].getSwitchDPID(), srcSp[0].getPort(), dstSp[0].getSwitchDPID(), dstSp[0].getPort(),	null);
	}


    @Override
    public Route getRoute(IPv4Address src, IPv4Address dst) {
        IDevice srcDev,	dstDev;
        Iterator<? extends IDevice> srcTmp = this.deviceService.queryDevices(MacAddress.NONE, null, src, IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);
        Iterator<? extends IDevice> dstTmp = this.deviceService.queryDevices(MacAddress.NONE, null, dst, IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);
        if(!srcTmp.hasNext() || !dstTmp.hasNext()){
            logger.error("cannot find device with MAC address set to {}", srcTmp.hasNext() ? dst : src);
            return null;
        } else {
            srcDev = srcTmp.next();
            dstDev = dstTmp.next();
            if(srcTmp.hasNext()  || dstTmp.hasNext()){
                logger.error("find multiple device with MAC set to {}", srcTmp.hasNext() ? src : dst);
                return null;
            }
        }

        SwitchPort[] srcSp = srcDev.getAttachmentPoints(),
                dstSp = dstDev.getAttachmentPoints();
        if(srcSp.length != 1){
            logger.error("find multiple attachment points {} for device {}", srcSp, src);
        }
        if(dstSp.length != 1){
            logger.error("find multiple attachment points {} for device {}", dstSp, dst);
        }
        return this.routingService.getRoute(srcSp[0].getSwitchDPID(), srcSp[0].getPort(), dstSp[0].getSwitchDPID(), dstSp[0].getPort(),	null);
    }
}
