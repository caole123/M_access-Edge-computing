package cn.edu.hnu.esnl.app.guoqixie.fs.tii3.alg;

import cn.edu.hnu.esnl.bean.Application;
import cn.edu.hnu.esnl.bean.Processor;
import cn.edu.hnu.esnl.bean.Task;
import cn.edu.hnu.esnl.bean.view.ESTEFT;
import cn.edu.hnu.esnl.bean.view.ESTLFT;
import cn.edu.hnu.esnl.bean.view.StartEndTime;
import cn.edu.hnu.esnl.service.TaskServiceBean;
import cn.edu.hnu.esnl.service.reliability.ReliabilityModelService;
import cn.edu.hnu.esnl.system.SystemValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class MUEECA {






        public static double exe(List<Processor> givenProcessorList, double p_comm, Application g0, double RGoalG, double deadline) {

            for (Task currentTask : g0.getScheduledSequence()) {


                Double taskMinRC = Double.MAX_VALUE;
                Double taskMaxRC = 0d;
                for (int i = 1; i < givenProcessorList.size(); i++) {
                    Processor p = givenProcessorList.get(i);

                    double min = ReliabilityModelService.NoError(currentTask, p);
                    if (min < taskMinRC)
                        taskMinRC = min;

                    double max = ReliabilityModelService.NoError(currentTask, p);

                    if (max > taskMaxRC)
                        taskMaxRC = max;

                }
                currentTask.setRMin(Math.pow(taskMinRC, 1));
                currentTask.setRMax(Math.pow(taskMaxRC, 1));
                currentTask.setResourceCost(0d);

            }



            for (int i = 0; i < g0.getScheduledSequence().size(); i++) {

                Task currentTask = g0.getScheduledSequence().get(i);

                double appAssignedRC = 1d;
                for (int x = 0; x < i; x++) {
                    appAssignedRC = appAssignedRC * g0.getScheduledSequence().get(x).getAR();

                }

                double appUnassignedRC = 1d;
                for (int y = i + 1; y < g0.getScheduledSequence().size(); y++) {
                    appUnassignedRC = appUnassignedRC * g0.getScheduledSequence().get(y).getRMax();

                }

                double taskGivenRC = RGoalG / appAssignedRC / appUnassignedRC;
                if(SystemValue.isPrint)
                    System.out.print("任务的最低可靠性献：" + currentTask.getRMin() + " 任务的最高可靠性贡献：" + currentTask.getRMax() );

                currentTask.setRMin(Math.max(currentTask.getRMin(), taskGivenRC));
                if(SystemValue.isPrint)
                    System.out.println(" 任务" + currentTask.getName() + " 任务的可靠性贡献目标:" + currentTask.getRMin());

                currentTask.getAssignedprocessor1().getTask$startEndTimeMap().remove(currentTask);
                StartEndTime time = new StartEndTime(currentTask.getLower_bound_st(), currentTask.getLower_bound_ft());
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

                    Set<Task> predTasks1 = predMap.keySet();//获取当前任务所有的前驱任务

                    for (Task predTask1 : predTasks1) {

                        Processor predP = predTask1.getAssignedprocessor1();//前驱任务的执行处理器

                        Double predAft = predTask1.getLower_bound_ft();//前驱任务的实际完成时间

                        double _c = 0d;

                        if (!p.getName().equals(predP.getName())) {//如果不在同一个处理器上，则有通信延迟

                            _c = currentTask.getPredTask$CommCostMap().get(predTask1);

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

                    estlfts.put(p, new ESTLFT(maxEst, minLft, wik));

                }

                double actualRC = 0d;
                double actualST = Double.MAX_VALUE;
                double actualFT = Double.MAX_VALUE;
                double actualCost =  Double.MAX_VALUE;
                Processor actualProcessor = null;

                for (int ii = 1; ii < givenProcessorList.size(); ii++) {

                    Processor p = givenProcessorList.get(ii);
                    ESTLFT estlef = estlfts.get(p); // 任务在处理上的最早开始最最早完成时间

                    double ast = estlef.getEst();


                    double aft = estlef.getLft();

                    double Treq = aft - ast;

                    double reliability = ReliabilityModelService.NoError(currentTask, p);


                    if (reliability < currentTask.getRMin()||Treq<estlef.getWik().intValue()) {
                        if(SystemValue.isPrint)
                            System.out.println("任务的可靠性目标或时间约束无法在处理器"+p.getName()+"满足："+currentTask.getName()+" reliability:"+reliability +"<"+ currentTask.getRMin());

                        continue;
                    }


                    ESTEFT esteft = TaskServiceBean.computeESTEFT(currentTask, p, "s"); // 在处理器上的�?早开始时�?

                    double cost1 = (esteft.getEft()-esteft.getEst())*p.getPrice();

                    Set<Task> predTasks = currentTask.getPredTask$CommCostMap().keySet();
                    double cost2= 0d;
                    for(Task predTask:predTasks){

                        //if(!predTask.getAssignedprocessor1().equals(currentTask.getAssignedprocessor1())){

                        cost2+= currentTask.getPredTask$CommCostMap().get(predTask)*p_comm;

                        //}

                    }

                    double cost = cost1+ cost2;

                    if(SystemValue.isPrint)
                        System.out.println("total cost:"+ cost + " on procesor "+p.getName() +" reliability:"+reliability);

                    if (cost< actualCost) {
                        actualCost =cost;
                        actualRC = reliability;
                        actualProcessor = p;
                        actualST = esteft.getEst();
                        actualFT = esteft.getEft();
                    }

                }

                StartEndTime startEndTime = new StartEndTime(actualST, actualFT);
                //actualProcessor.getTask$startEndTimeMap().put(currentTask, startEndTime);
                //actualProcessor.getStartEndTime$TaskMap().put(startEndTime, currentTask);
                //actualProcessor.getTask$availTimeMap().put(currentTask, actualFT);

                currentTask.setAssignedprocessor1(actualProcessor);
                currentTask.setLower_bound_st(actualST);
                currentTask.setLower_bound_ft(actualFT);

                currentTask.setAR(actualRC);
                currentTask.setResourceCost(actualCost);


                if(SystemValue.isPrint){
                    System.out.println("task " + currentTask.getName() + " AST: " + currentTask.getLower_bound_st() + " AFT: " + currentTask.getLower_bound_ft() + " processor "
                            + currentTask.getAssignedprocessor1() + "  actual RC:" + currentTask.getAR() + " actualCost:" + currentTask.getResourceCost());
                    System.out.println("=========");
                }

            }

            Processor Localprocessor = new Processor();
            Localprocessor.setName("p_1");
            double LocalC = 0d;
            int LocalTaskNumber = 0;

            double totalReliability = 1d;
            double totalCost = 0d;
            double executime = 0;
            for (int i = 0; i < g0.getScheduledSequence().size(); i++) {

                Task currentTask = g0.getScheduledSequence().get(i);
                if (currentTask.getAssignedprocessor1().getName().equals(Localprocessor.getName())){
                    LocalTaskNumber++;
                    LocalC += currentTask.getResourceCost();

                }

                if (currentTask.getLower_bound_ft()>executime){
                    executime = currentTask.getLower_bound_ft();
                }



                totalReliability = totalReliability * currentTask.getAR();

                totalCost += currentTask.getResourceCost();

            }

            //System.out.println("本地终端的能耗为"+LocalC);
            System.out.println(" Local Task:" + LocalTaskNumber);
            System.out.println(" TOTAL reliability:" + totalReliability);
            System.out.println(" TOTAL totalCost:" + totalCost);
            System.out.println(" execution time:"+ executime);
            System.out.println("");
            return LocalC;
        }


    }
