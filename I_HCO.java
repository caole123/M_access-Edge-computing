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
import cn.edu.hnu.esnl.util.DeepCopyUtil;
import cn.edu.hnu.esnl.util.DoubleUtil;

/**
 * @author xgq E-mail:xgqman@126.com
 * 
 * 
 */
public class I_HCO {

	public static Double exe(List<Processor> givenProcessorList, Integer[][] compMartrix, Integer[][] commMartrix, double rgoal,Double deadline) throws IOException, ClassNotFoundException {
		List<String> minProcessors = new ArrayList<String>();

		double minTotalHardwareCost = Double.MAX_VALUE;
		int processorNumber = 0;
		double makespan = 0d;
		double reliability = 0d;
		long st = System.currentTimeMillis();
		
		Application finalG = null;
		while (true) {

			// HEFT能耗结果
			List<Processor> givenProcessorList0 = DeepCopyUtil.copy(givenProcessorList);
			for (Processor processor : givenProcessorList0) {

				if (minProcessors.contains(processor.getName()))
					processor.setOpen(false);
			}

			Application g0 = new Application("F_1", Criticality.S3);
			ApplicationServiceBean.init(g0, givenProcessorList0, compMartrix, commMartrix, 0d, 90d); // 69
			new HEFTScheduler().scheduling(g0, givenProcessorList0); // 80
			makespan = g0.getLower_bound();
			 reliability = g0.getReliability(); // 可靠性增强
			if (makespan <= deadline&&reliability>=rgoal) {
				finalG = g0;
			
				
				double newTotalHardwareCost = 0;

				processorNumber = 0;
				for (int j = 1; j < givenProcessorList0.size(); j++) {

					Processor p = givenProcessorList0.get(j);
					if (p.getOpen() == false)
						continue;
					processorNumber++;

					newTotalHardwareCost = newTotalHardwareCost + p.getPrice();

				}

				minTotalHardwareCost = newTotalHardwareCost;

				// 接下来考虑关闭哪个处理器，关闭任务数最小的，如果任务数相同，则关闭利用率低的
				List<TurnOFF> turnoffs = new ArrayList<TurnOFF>();
				for (Processor processor : givenProcessorList0) {

					if (processor.getName().equals("p_0"))
						continue;

					if (!processor.getOpen())
						continue;

					turnoffs.add(new TurnOFF(processor, processor.getPrice().longValue(), processor.getPrice()));

				}
				Collections.sort(turnoffs, new Comparator<TurnOFF>() {

					public int compare(TurnOFF arg0, TurnOFF arg1) {

						 int result = (int) (arg1.getLength()- arg0.getLength());
						 return result;
						//int result = arg0.getLength().compareTo(arg1.getLength());
						//if (result == 0)
						//	result = arg0.getEnergy().compareTo(arg1.getEnergy());
						//return result;
					}

				});

				minProcessors.add(turnoffs.get(0).getProcessor().getName());

			} else {

				break;
			}

		}
		long ft = System.currentTimeMillis();

		
		System.out.println("ICHO-generated total COST is " + DoubleUtil.format(minTotalHardwareCost));
		System.out.println("ICHO-generated computation value is " + (ft - st) / 1000 + " s");
		System.out.println("ICHO-generated turned-on processor number is " + processorNumber);
		System.out.println("ICHO-generated reliability is " + finalG.getReliability());
		System.out.println("ICHO-generated response time is " + finalG.getLower_bound());
		minProcessors.remove(minProcessors.size()-1);
		System.out.println("ICHO-generated turned off is " + minProcessors);
		System.out.println("");
		return DoubleUtil.format(minTotalHardwareCost);
	}

}
