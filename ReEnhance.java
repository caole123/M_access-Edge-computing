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
public class ReEnhance {

	

	public static double exe(List<Processor> givenProcessorList, Double deadline, Application gx, boolean print) {
		double heftXReliability;
		// 可靠性增强方法
		List<Task> upwardTaskList = new ArrayList<Task>();

		for (Task task : gx.getScheduledSequence()) {
			double generatedR = ReliabilityModelService.NoError(task, task.getAssignedprocessor1());//得到分配在处理器上的任务
                      //math.exp(task,task.getAssignedprocessor1());
			task.setAR(generatedR);
			upwardTaskList.add(task);
			//upwardTaskList.add(0, task);
		}

		double totoalR = 1;
		for (int i = 0; i < upwardTaskList.size(); i++) {

			Task currentTask = upwardTaskList.get(i);

			Double selectedR = 0d;

			Processor selectedProcessor = null;
			Double selectedAST = null;
			Double selectedAFT = null;

			currentTask.getAssignedprocessor1().getTask$startEndTimeMap().remove(currentTask);
			StartEndTime time = new StartEndTime(currentTask.getLower_bound_st(), currentTask.getLower_bound_ft());
			//if (print)
			//	System.out.println("currentTask:" + currentTask.getName() +" processor:" + currentTask.getAssignedprocessor1().getName()+ " reliability " + DoubleUtil.format6(currentTask.getAR()) + " delete time:" + time);

			//currentTask.getAssignedprocessor1().getStartEndTime$TaskMap().remove(time);
			//currentTask.getAssignedprocessor1().getTask$availTimeMap().remove(currentTask);

			currentTask.setAssignedprocessor1(null);
			currentTask.setLower_bound_st(0d);
			currentTask.setLower_bound_ft(0d);
 
			// 获得最早开始时间和最迟完成时间
			LinkedHashMap<Processor, ESTLFT> estlfts = new LinkedHashMap<Processor, ESTLFT>();

			for (int jj = 1; jj < givenProcessorList.size(); jj++) {

				Processor p = givenProcessorList.get(jj);

				if (p.getOpen() == false)
					continue;

				double wik = currentTask.getProcessor$CompCostMap().get(p);//当前任务在当前处理器上的执行时间

				// 设置AST

				double maxEst = 0d;

				LinkedHashMap<Task, Integer> predMap = currentTask.getPredTask$CommCostMap();//当前任务与它的前驱任务，通信代价

				Set<Task> predTasks = predMap.keySet();//获取当前任务所有的前驱任务

				for (Task predTask : predTasks) {

					Processor predP = predTask.getAssignedprocessor1();//前驱任务的执行处理器

					Double predAft = predTask.getLower_bound_ft();//前驱任务的实际完成时间

					double _c = 0d;

					if (!p.getName().equals(predP.getName())) {//如果不在同一个处理器上，则有通信延迟

						_c = currentTask.getPredTask$CommCostMap().get(predTask);

					}

					double est = predAft + _c;//当前任务的最早开始时间等于前驱任务的实际完成时间+通信代价
					
					if (est > maxEst) {

						maxEst = est;

					}

				}
				
				LinkedHashMap<Task, Integer> succMap = currentTask.getSuccTask$CommCostMap();//当前任务与后继任务的通信代价

				Set<Task> succTasks = succMap.keySet();//得到当前任务的后继任务
				double minLft = Double.MAX_VALUE;
				for (Task succTask : succTasks) {
					Processor succP = succTask.getAssignedprocessor1();//后继任务的处理器
					Double succAst = succTask.getLower_bound_st();//后继任务的实际开始时间
					double _c = 0d;
					if (!p.getName().equals(succP.getName())) {
						_c = currentTask.getSuccTask$CommCostMap().get(succTask);//如果在同一处理器，则没有通信代价
					}

					double lft = succAst - _c;//当前任务的最迟完成时间
					

					if (lft < minLft)
						minLft = lft;
					
					
					
				}

				if (currentTask.getIsExit()) {
					minLft = deadline;
				}
				
				if(currentTask.getName().endsWith("n_?")){
					
					System.out.println(currentTask.getName()+", p:"+p.getName()+", est:"+maxEst);
					System.out.println(currentTask.getName()+", p:"+p.getName()+", lft:"+minLft);
				}
				

				estlfts.put(p, new ESTLFT(maxEst, minLft, wik));

			}

			LinkedHashMap<Processor, List<Slack>> processorSlacks = new LinkedHashMap<Processor, List<Slack>>();

			
			
			// 计算slacks
			for (int jj = 1; jj < givenProcessorList.size(); jj++) {

				Processor p = givenProcessorList.get(jj);
				if (p.getOpen() == false)
					continue;
				// 是否可以插入？

				// 计算slacks
				Set<StartEndTime> times1 = p.getStartEndTime$TaskMap().keySet();

				List<StartEndTime> times = new ArrayList<StartEndTime>();
				for (StartEndTime time1 : times1) {

					times.add(time1);

				}
				Collections.sort(times);

				List<Slack> slacks = new ArrayList<Slack>();
				if (times.size() > 0) {

					Slack slack = new Slack(0d, times.get(0).getStartTime());
					if (slack.getStartTime().doubleValue() != slack.getEndTime().doubleValue()) {

						slacks.add(slack);
					}

				}

				for (int k = 0; k < times.size() - 1; k++) {

					StartEndTime set1 = times.get(k);
					StartEndTime set2 = times.get(k + 1);

					Slack slack = new Slack(set1.getEndTime(), set2.getStartTime());
					if (slack.getStartTime().intValue() == slack.getEndTime().intValue())
						continue;
					slacks.add(slack);
				}

				if (times.size() > 0) {

					Slack slack = new Slack(times.get(times.size() - 1).getEndTime(), deadline);
					slacks.add(slack);

				}
				if (times.size() == 0) {

					Slack slack = new Slack(0d, deadline);
					slacks.add(slack);

				}

				processorSlacks.put(p, slacks);

			}
			if(currentTask.getName().endsWith("n_?")){
			for(Processor pv:processorSlacks.keySet()){
				
				System.out.println(pv.getName()+" 空隙: "+processorSlacks.get(pv));
				
			}
			}
			for (int jj = 1; jj < givenProcessorList.size(); jj++) {

				Processor p = givenProcessorList.get(jj);
				if (p.getOpen() == false)
					continue;
				ESTLFT estlef = estlfts.get(p); // 任务在处理上的最早开始最最早完成时间

				List<Slack> slacks = processorSlacks.get(p);

				for (Slack slack : slacks) {

					double ast = estlef.getEst();
				//Math.max(estlef.getEst(), slack.getStartTime());

					double aft = estlef.getLft();
				//Math.min(estlef.getLft(), slack.getEndTime());
					
					if ((aft - ast) <0) {

						continue;
					}
					
					//if(currentTask.getName().endsWith("n_?")){
					//	System.out.println(p.getName()+", ast:"+ast+", aft:"+aft);
					//}
					if ((aft - ast) < estlef.getWik().intValue()) {

						continue;
					}

					ast = aft - estlef.getWik();

					double generatedR = ReliabilityModelService.NoError(currentTask, p);
					//System.out.println("processor:" + p + " new generatedR " + DoubleUtil.format6(generatedR));
					if(currentTask.getName().endsWith("n_?")){
						System.out.println(p.getName()+", generatedR:"+generatedR);
					}
					if (generatedR >= selectedR) {

						selectedR = generatedR;
						selectedProcessor = p;
						selectedAST = ast;
						selectedAFT = aft;

					}

				}

			}

			currentTask.setLower_bound_st(selectedAST);
			currentTask.setLower_bound_ft(selectedAFT);

			currentTask.setAR(selectedR);

			StartEndTime startEndTime = new StartEndTime(selectedAST, selectedAFT);

			currentTask.setAssignedprocessor1(selectedProcessor);

			//if (print){
			//	System.out.println(currentTask.getName()+" processor:" + selectedProcessor  + " selectedASTAFT:[" + (selectedAST-19) + "," + (selectedAFT-19) + "]"+ " new reliabilty " + DoubleUtil.format6(selectedR));
			//	System.out.println("");
			//}
			selectedProcessor.getTask$startEndTimeMap().put(currentTask, startEndTime);
			selectedProcessor.getStartEndTime$TaskMap().put(startEndTime, currentTask);
			selectedProcessor.getTask$availTimeMap().put(currentTask, selectedAFT);

			totoalR = totoalR * selectedR;

		}
		heftXReliability = totoalR;
		return heftXReliability;
	}
}
