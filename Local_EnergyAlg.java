package cn.edu.hnu.esnl.app.guoqixie.fs.tii3.alg;
import java.io.IOException;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;


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
public class Local_EnergyAlg {
    public static double exe(Application G,List<Task> givenTaskList,List<Processor> givenProcessorList ) throws IOException, ClassNotFoundException {

        List<Processor> givenProcessorList0 = DeepCopyUtil.copy(givenProcessorList);
        Application g0 = new Application("F_1", Criticality.S3);
        //ApplicationServiceBean.init(g0, givenProcessorList0, compMartrix, commMartrix, 0d, 90d); // 69
       // new HEFTScheduler().scheduling(g0, givenProcessorList0,print); // 80
        int processorNumber = 0;
        long st = System.currentTimeMillis();
        for (int j = 0; j < givenProcessorList0.size(); j++) {
            Processor p_j = givenProcessorList0.get(j);
            if (p_j.getOpen() == false)
                continue;
            processorNumber++;

        }
        double TotalRunTime = 0;
        int TaskNumber = 0;
        int LocalTask = 0;
        int RunProcessorTask = 0;
        double TotalData = 0;
        double TotalreqT = 0;
        //分支1
        List<Task> givenTaskList0 = DeepCopyUtil.copy(givenTaskList);
        for (int i = 0; i< givenTaskList0.size();i++ ){
            Task v_i = givenTaskList0.get(i);
            TaskNumber++;
            TotalRunTime = TotalRunTime + v_i.getRunCpuNumber();
            TotalData = TotalData + v_i.getData();
            TotalreqT = TotalreqT + v_i.getreqT();
            if(v_i.getRunCpuNumber()/G.getLocalCpuNumber()>v_i.getreqT()) {
                RunProcessorTask++;
                v_i.setOpen(false);
                System.out.println("分支1后在MEC服务器上运行的任务: "+v_i.getName());
            }
            else{
                System.out.println("分支1后在本地执行的任务:"+v_i.getName());
            }
         }
         //分支2
        int J;
        double TotalTaskRunCpuNumber = 0;
        double TotalTaskData = 0;
        double TaskRunCpuNumber[]=new double[givenTaskList0.size()+1];
        double TaskData[]=new double[givenTaskList0.size()+1];
         for (int i = 0;i<givenTaskList0.size();i++){
            Task verV_i = givenTaskList0.get(i);
            if (verV_i.getOpen() == false){//表示在MEC上运行的任务
                continue;
            }
            TaskData[i] = verV_i.getData();//去除分支1中在MEC上运行的任务后，其余任务的数据规模数组
            TaskRunCpuNumber[i] = verV_i.getRunCpuNumber();//其余任务的所需计算资源数组
         }
         Arrays.sort(TaskRunCpuNumber,0,givenTaskList0.size());//从大到小排序
        Arrays.sort(TaskData,0,givenTaskList0.size());
        for (int i = 0;i<TaskRunCpuNumber.length;i++){
            TotalTaskRunCpuNumber = TotalTaskRunCpuNumber+TaskRunCpuNumber[i];
            if (TotalTaskRunCpuNumber>G.getLocalCpuNumber()){
                J = i-1;//得到满足分支2最小的下标
                System.out.println("J = "+J);
                break;
            }
        }
        for (int i = 0; i< givenTaskList0.size();i++){
            Task Ver_i = givenTaskList0.get(i);//
            if (Ver_i.getOpen()==false){
                continue;
            }
            for (int j = 0;j<givenTaskList0.size();j++){
                for (int k = 0; k<givenTaskList0.size();k++){

            if (Ver_i.getData()==TaskData[j]&&Ver_i.getRunCpuNumber()!=TaskRunCpuNumber[k]){
                        Ver_i.setOpen(false);
                    }
                    if (Ver_i.getData()==TaskData[j]&&Ver_i.getRunCpuNumber()==TaskRunCpuNumber[k])    {
                                      for (int h = 0;h<givenProcessorList0.size();h++)
                                      {         Processor p_h = givenProcessorList0.get(h);
                                          if (g0.getEnergypercent()*(Ver_i.getRunCpuNumber()/g0.getLocalCpuNumber())>g0.getTrPower()*(Ver_i.getData()/g0.getUpdataRate())){
                                              Ver_i.setOpen(false);
                                          }
                                      }
                    }
                }
            }
        }



        long ft = System.currentTimeMillis();

        System.out.println("local_energy computation value is " + (ft - st) / 1000 + " s");
        for (int i = 0; i<givenTaskList0.size();i++){
            if (givenTaskList0.get(i).getOpen() == false){
                LocalTask++;
                System.out.println("loaclTask is "+givenTaskList0.get(i).getName());

            }
        }
        System.out.println("Tota=l Task number is "+ TaskNumber);
        System.out.println("MEC processor number is " + processorNumber);
        System.out.println("Local Task number is "+ LocalTask);
        System.out.println("Run MEC Task number is "+ (TaskNumber - LocalTask));
        for (int i = 0;i<givenTaskList0.size();i++)
        {
            givenTaskList0.get(i).setOpen(true);
        }
    return 0;
    }
}
