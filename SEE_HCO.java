package cn.edu.hnu.esnl.app.guoqixie.fs.tii3.alg;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;


import cn.edu.hnu.esnl.bean.Application;
import cn.edu.hnu.esnl.bean.Criticality;
import cn.edu.hnu.esnl.bean.Processor;
import cn.edu.hnu.esnl.bean.Task;
import cn.edu.hnu.esnl.bean.view.ESTEFT;
import cn.edu.hnu.esnl.bean.view.PEFESTEFT;
import cn.edu.hnu.esnl.bean.view.PEFESTEFT2;
import cn.edu.hnu.esnl.bean.view.PEFESTEFT3;
import cn.edu.hnu.esnl.bean.view.SafeEnergytResult;
import cn.edu.hnu.esnl.bean.view.StartEndTime;
import cn.edu.hnu.esnl.bean.view.TOTAL_ENERGY;
import cn.edu.hnu.esnl.bean.view.TurnOFF;
import cn.edu.hnu.esnl.schedule.assitant.HEFTScheduler;
import cn.edu.hnu.esnl.schedule.assitant.RSScheduler;
import cn.edu.hnu.esnl.scheduler.*;
import cn.edu.hnu.esnl.service.ApplicationServiceBean;
import cn.edu.hnu.esnl.service.JSAPerformanceService;
import cn.edu.hnu.esnl.service.MMPerformanceService;
import cn.edu.hnu.esnl.service.PerformanceService;
import cn.edu.hnu.esnl.service.TaskServiceBean;
import cn.edu.hnu.esnl.service.energy.EnergyService;
import cn.edu.hnu.esnl.service.graph.Gaussian;
import cn.edu.hnu.esnl.system.SystemValue;
import cn.edu.hnu.esnl.util.DeepCopyUtil;
import cn.edu.hnu.esnl.util.DoubleUtil;

/**
 * @author xgq E-mail:xgqman@126.com
 * 
 * 
 */
public class SEE_HCO {

	public static Double exe(List<Processor> givenProcessorList, Integer[][] compMartrix, Integer[][] commMartrix, Double rgoal, Double deadline) throws IOException, ClassNotFoundException {
		List<String> toClosedProcessors = new ArrayList<String>();

		double minTotalEnergy = 100; // 初始的成本是HEFT的成本
		int processorNumber = 0;
		double makespan = 0d;
		double reliability = 0d;
		long st = System.currentTimeMillis();
		Application finalG = null;
		// 首先对每个处理器调度一次
		for (int j = 1; j < givenProcessorList.size(); j++) {
			Processor verP = givenProcessorList.get(j);

			List<Processor> givenProcessorListx = DeepCopyUtil.copy(givenProcessorList);
			for (int k = 1; k < givenProcessorListx.size(); k++) {
				Processor processorx = givenProcessorListx.get(k);
				if (toClosedProcessors.contains(processorx.getName()))
					processorx.setOpen(false);
				if (processorx.getName().equals(verP.getName()))
					processorx.setOpen(false);
			}

			Application gx = new Application("F_x", Criticality.S3);
			ApplicationServiceBean.init(gx, givenProcessorListx, compMartrix, commMartrix, 0d, 90d); // 69
			new HEFTScheduler().scheduling(gx, givenProcessorListx); // 80
			finalG = gx;
			double heftXMakespan = gx.getLower_bound();
			double heftXReliability = gx.getReliability();
			
			
			//System.out.print("ESE预关闭的处理器："+verP.getName() + "   heftXMakespan:" + heftXMakespan + "   heftXReliability:" + heftXReliability);
			
			if (heftXMakespan <= deadline) {
				heftXReliability = ReEnhance.exe(givenProcessorListx, deadline, gx,false);
				
				//System.out.println(" Enhanced heftXReliability:" + heftXReliability);
				
				if (heftXReliability >= rgoal) {
					double heftXTotalenergy = 0;

					for (int k = 1; k < givenProcessorListx.size(); k++) {
						Processor px = givenProcessorListx.get(k);
						if (px.getOpen() == false)
							continue;

						heftXTotalenergy = heftXTotalenergy + px.getPrice();
					}
					verP.setProcessorEnergy(heftXTotalenergy);
					verP.setProcessorSL(heftXMakespan);
					verP.setProcessorReliability(heftXReliability);

					minTotalEnergy = heftXTotalenergy;
					finalG = gx;
				}
				else{
					verP.setProcessorEnergy(Double.MAX_VALUE);
					verP.setProcessorSL(Double.MAX_VALUE);
					verP.setProcessorReliability(Double.MAX_VALUE);
					
				}
			} else {
				//System.out.println("");
				verP.setProcessorEnergy(Double.MAX_VALUE);
				verP.setProcessorSL(Double.MAX_VALUE);
				verP.setProcessorReliability(Double.MAX_VALUE);
			}
		}
		if(SystemValue.isPrint)
			System.out.println("------------------------------");
		
	
		List<String> closedProcessors = new ArrayList<String>();
		while (true) {
			
			if(SystemValue.isPrint)
				System.out.println("可能关闭的处理器集:" + toClosedProcessors);
			// HEFT能耗结果
			List<Processor> givenProcessorList0 = DeepCopyUtil.copy(givenProcessorList);
			for (Processor processor : givenProcessorList0) {

				if (toClosedProcessors.contains(processor.getName()))
					processor.setOpen(false);
			}

			Application g0 = new Application("F_1", Criticality.S3);
			ApplicationServiceBean.init(g0, givenProcessorList0, compMartrix, commMartrix, 0d, 90d); // 69
			new HEFTScheduler().scheduling(g0, givenProcessorList0); // 80
			makespan = g0.getLower_bound();
			reliability = g0.getReliability();
		
			
			if (makespan <= deadline) {
				reliability = ReEnhance.exe(givenProcessorList0, deadline, g0,false);
				
				if (reliability >= rgoal) { // 可靠性要超过目标值，才行
					double newTotalEnergy = 0;
					processorNumber = 0;
					for (int j = 1; j < givenProcessorList0.size(); j++) {

						Processor p = givenProcessorList0.get(j);
						if (p.getOpen() == false)
							continue;
						processorNumber++;

						newTotalEnergy = newTotalEnergy + p.getPrice();

					}
				
					minTotalEnergy = newTotalEnergy;
					finalG = g0;
					//System.out.println( "   makespan:" + makespan + "   reliability:" + reliability+" cost:"+minTotalEnergy);
				
					if(toClosedProcessors.size()>0)
						closedProcessors.add(toClosedProcessors.get(toClosedProcessors.size()-1));
				}else{
					
				
					
				}

				// 接下来考虑关闭哪个处理器，关闭任能量最小的
				double minEnergy = Double.MAX_VALUE;
				String minName = null;
				for (int k = 1; k < givenProcessorList.size(); k++) {
					Processor processorx = givenProcessorList.get(k);
					if (toClosedProcessors.contains(processorx.getName()))
						continue;

					if (processorx.getProcessorSL() > deadline) {
						continue;
					}

					if (processorx.getProcessorReliability() < rgoal) {
						continue;
					}

					if (processorx.getProcessorEnergy() < minEnergy) {
						minEnergy = processorx.getProcessorEnergy();
						minName = processorx.getName();
					}
				}
				if (minName == null)
					break;
				//System.out.println("试图关闭的处理器："+minName);
				toClosedProcessors.add(minName);
			//	System.out.println("minProcessors:"+minProcessors);

			} else {
				
				break;
			}

		}

		long ft = System.currentTimeMillis();
		double scheduleST = Double.MAX_VALUE;
		double scheduleFT = 0;
		for(int i=0;i<finalG.getScheduledSequence().size();i++){
			
			if(finalG.getScheduledSequence().get(i).getLower_bound_st()<scheduleST)
				scheduleST =finalG.getScheduledSequence().get(i).getLower_bound_st();
			
			if(finalG.getScheduledSequence().get(i).getLower_bound_ft()>scheduleFT)
				scheduleFT =finalG.getScheduledSequence().get(i).getLower_bound_ft();
			
		}
		finalG.setLower_bound(scheduleFT-scheduleST);
		//System.out.println("------------------------------");
		System.out.println("SEHCO-generated total COST is " + DoubleUtil.format(minTotalEnergy));
		System.out.println("SEHCO-generated computation value is " + (ft - st) / 1000 + " s");
		System.out.println("SEHCO-generated turned-on processor number is " + processorNumber);
		System.out.println("SEHCO-generated reliability is " + finalG.getReliability());
		System.out.println("SEHCO-generated response time is " + finalG.getLower_bound());
		System.out.println("SEHCO-generated turned off is " + closedProcessors);
		System.out.println("");
		return minTotalEnergy;
	}

}
