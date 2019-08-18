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
import cn.edu.hnu.esnl.bean.view.ESTLFT;
import cn.edu.hnu.esnl.bean.view.PEFESTEFT;
import cn.edu.hnu.esnl.bean.view.PEFESTEFT2;
import cn.edu.hnu.esnl.bean.view.PEFESTEFT3;
import cn.edu.hnu.esnl.bean.view.SafeEnergytResult;
import cn.edu.hnu.esnl.bean.view.Slack;
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
import cn.edu.hnu.esnl.service.reliability.ReliabilityModelService;
import cn.edu.hnu.esnl.system.SystemValue;
import cn.edu.hnu.esnl.util.DeepCopyUtil;
import cn.edu.hnu.esnl.util.DoubleUtil;

/**
 * @author xgq E-mail:xgqman@126.com
 * 
 * 
 */
public class E_HCO {

	public static double exe(List<Processor> givenProcessorList, Integer[][] compMartrix, Integer[][] commMartrix, Double rgoal, Double deadline, boolean print ) throws IOException, ClassNotFoundException {

		List<Processor> givenProcessorList0 = DeepCopyUtil.copy(givenProcessorList);
		Application g0 = new Application("F_1", Criticality.S3);
		ApplicationServiceBean.init(g0, givenProcessorList0, compMartrix, commMartrix, 0d, 90d); // 69
		new HEFTScheduler().scheduling(g0, givenProcessorList0,print); // 80

		double totalCost = 0;
		int processorNumber = 0;
		long st = System.currentTimeMillis();
		for (int j = 1; j < givenProcessorList0.size(); j++) {     //得到总的处理器个数 和总的处理器硬件成本
			Processor p = givenProcessorList0.get(j);
			if (p.getOpen() == false)
				continue;
			processorNumber++;
			totalCost = totalCost + p.getPrice();
		}

		List<String> minProcessors = new ArrayList<String>();     //创建一个能耗最小的处理器数列
	

		Application finalG = g0;    //copy g0的复制finalG        

		while (true) {
			//System.out.println("------------------------------");
			//count++;
			// System.out.println("count:" + count);
			// 选一个能够满足Deadline且能耗最小的处理器关掉
			Processor minProcessor = null;

			for (int j = 1; j < givenProcessorList0.size(); j++) {
				Processor verP = givenProcessorList0.get(j);   //预关闭处理器

				if (!verP.getOpen())
					continue;

				List<Processor> givenProcessorListx = DeepCopyUtil.copy(givenProcessorList);
				int validProcessorNum = 0;
				for (int k = 1; k < givenProcessorListx.size(); k++) {
					Processor processorx = givenProcessorListx.get(k);
					if (minProcessors.contains(processorx.getName()))
						processorx.setOpen(false);
					else if (processorx.getName().equals(verP.getName()))
						processorx.setOpen(false);
					else
						validProcessorNum++;
				}
				if (validProcessorNum == 0)
					break;

				Application gx = new Application("F_x", Criticality.S3);
				ApplicationServiceBean.init(gx, givenProcessorListx, compMartrix, commMartrix, 0d, 90d); // 69
				new HEFTScheduler().scheduling(gx, givenProcessorListx,print); // 80

				double heftXMakespan = gx.getLower_bound();
				double heftXReliability = gx.getReliability(); // 可靠性增强

				//System.out.println("预关闭的处理器："+verP.getName() + "   heftXMakespan:" + heftXMakespan + "   heftXReliability:" + heftXReliability);
				
				
				if (heftXMakespan <= deadline) {
				
					if (heftXReliability >= rgoal) {

						double heftXTotalenergy = 0;

						int heftProcessorNumber = 0;
						for (int k = 1; k < givenProcessorListx.size(); k++) {
							Processor px = givenProcessorListx.get(k);
							if (px.getOpen() == false)
								continue;
							heftProcessorNumber++;
							heftXTotalenergy = heftXTotalenergy + px.getPrice();
						}
						//System.out.println("预关闭的处理器："+verP.getName() + "   heftXTotalCost:"+heftXTotalenergy);
						
						
						if (heftXTotalenergy < totalCost && heftXReliability >= rgoal) {

							minProcessor = verP;
							totalCost = heftXTotalenergy;
							processorNumber = heftProcessorNumber;

							finalG = gx;
						}
					}

				}

			}

			// System.out.println("minProcessors:" + minProcessors);
			if (minProcessor != null) {
				minProcessors.add(minProcessor.getName());
				minProcessor.setOpen(false);
			} else {

				break;
			}

		}
		long ft = System.currentTimeMillis();

		System.out.println("E_HCO-generated total COST is " + DoubleUtil.format(totalCost));
		System.out.println("E_HCO-generated computation value is " + (ft - st) / 1000 + " s");
		System.out.println("E_HCO-generated turned-on processor number is " + processorNumber);
		System.out.println("E_HCO-generated reliability is " + finalG.getReliability());
		System.out.println("E_HCO-generated response time is " + finalG.getLower_bound());
		System.out.println("E_HCO-generated turned off is " + minProcessors);

		System.out.println("");
		return DoubleUtil.format(totalCost);
	}

	
}
