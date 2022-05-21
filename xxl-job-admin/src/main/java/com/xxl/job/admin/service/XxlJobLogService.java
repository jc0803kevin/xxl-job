package com.xxl.job.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.admin.dao.XxlJobLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class XxlJobLogService extends ServiceImpl<XxlJobLogMapper, XxlJobLog> {

    @Autowired
    private XxlJobRegistryService xxlJobRegistryService;


    public IPage<XxlJobLog> page(@Param("offset") int offset,
                                 @Param("pagesize") int pagesize,
                                 @Param("jobGroup") int jobGroup,
                                 @Param("jobId") int jobId,
                                 @Param("triggerTimeStart") Date triggerTimeStart,
                                 @Param("triggerTimeEnd") Date triggerTimeEnd,
                                 @Param("logStatus") int logStatus) {
        QueryWrapper<XxlJobLog> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<XxlJobLog> lambda = queryWrapper.lambda();
        if (jobId == 0 && jobGroup > 0) {
            lambda.eq(XxlJobLog::getJobGroup, jobGroup);
        }
        if (jobId > 0) {
            lambda.eq(XxlJobLog::getJobId, jobId);
        }
        if (null != triggerTimeStart) {
            lambda.ge(XxlJobLog::getTriggerTime, triggerTimeStart);
        }
        if (null != triggerTimeEnd) {
            lambda.le(XxlJobLog::getTriggerTime, triggerTimeEnd);
        }
        if (logStatus == 1) {
            lambda.eq(XxlJobLog::getHandleCode, 200);
        } else if (logStatus == 2) {
            lambda.notIn(XxlJobLog::getTriggerCode, 0, 200).or()
                    .notIn(XxlJobLog::getHandleCode, 0, 200);
        } else if (logStatus == 3) {
            lambda.eq(XxlJobLog::getTriggerCode, 200)
                    .eq(XxlJobLog::getHandleCode, 0);
        }
        queryWrapper.orderByDesc(" trigger_time");
        IPage<XxlJobLog> page = new Page<>(offset / pagesize + 1, pagesize);
        return this.page(page, queryWrapper);
    }

    public boolean delete(@Param("jobId") int jobId) {
        QueryWrapper<XxlJobLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(XxlJobLog::getJobId, jobId);
        return remove(queryWrapper);
    }

    public Map<String, Object> findLogReport(@Param("from") Date from,
                                             @Param("to") Date to) {
        QueryWrapper<XxlJobLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().between(XxlJobLog::getTriggerTime, from, to);
        List<XxlJobLog> ilist = list(queryWrapper);
        int triggerDayCount = ilist.size();
        int triggerDayCountRunning = 0;
        int triggerDayCountSuc = 0;
        for (XxlJobLog xxlJobLog : ilist) {
            if (xxlJobLog.getTriggerCode() == 0 || xxlJobLog.getTriggerCode() == 200 && xxlJobLog.getHandleCode() == 0) {
                triggerDayCountRunning++;
            }
            if (xxlJobLog.getHandleCode() == 200) {
                triggerDayCountSuc++;
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("triggerDayCount", triggerDayCount);
        map.put("triggerDayCountRunning", triggerDayCountRunning);
        map.put("triggerDayCountSuc", triggerDayCountSuc);
        return map;
    }

    public List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
                                      @Param("jobId") int jobId,
                                      @Param("clearBeforeTime") Date clearBeforeTime,
                                      @Param("clearBeforeNum") int clearBeforeNum,
                                      @Param("pagesize") int pagesize) {
        QueryWrapper<XxlJobLog> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<XxlJobLog> lambda = queryWrapper.lambda();
        if (jobGroup > 0) {
            lambda.eq(XxlJobLog::getJobGroup, jobGroup);
        }
        if (jobId > 0) {
            lambda.eq(XxlJobLog::getJobId, jobId);
        }
        if (null != clearBeforeTime) {
            lambda.le(XxlJobLog::getTriggerTime, clearBeforeTime);
        }
        if (clearBeforeNum > 0) {
            QueryWrapper<XxlJobLog> idqw = new QueryWrapper<>();
            LambdaQueryWrapper<XxlJobLog> lambda2 = idqw.lambda();
            if (jobGroup > 0) {
                lambda2.eq(XxlJobLog::getJobGroup, jobGroup);
            }
            if (jobId > 0) {
                lambda2.eq(XxlJobLog::getJobId, jobId);
            }
            idqw.orderByDesc(" trigger_time");
            IPage<XxlJobLog> iPage = new Page<>(0, clearBeforeNum);
//            idqw.last(" limit 0," + clearBeforeNum);
            iPage = page(iPage, idqw);
            List<XxlJobLog> idlist = iPage.getRecords();
            List<Long> collect = idlist.stream().map(XxlJobLog::getId).collect(Collectors.toList());
            lambda.notIn(XxlJobLog::getId, collect);
        }
        queryWrapper.orderByAsc("id");
        IPage<XxlJobLog> iPage = new Page<>(0, pagesize);
//        queryWrapper.last(" limit " + pagesize);
        iPage = page(iPage, queryWrapper);
        List<XxlJobLog> ilist = iPage.getRecords();
        return ilist.stream().map(XxlJobLog::getId).collect(Collectors.toList());
    }

    public boolean clearLog(@Param("logIds") List<Long> logIds) {
        QueryWrapper<XxlJobLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(XxlJobLog::getId, logIds);
        return remove(queryWrapper);
    }

    public List<Long> findFailJobLogIds(@Param("pagesize") int pagesize) {
        QueryWrapper<XxlJobLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(XxlJobLog::getAlarmStatus, 0)
                .notIn(XxlJobLog::getTriggerCode, 0, 200)
                .notIn(XxlJobLog::getHandleCode, 0, 200);
        queryWrapper.orderByAsc("id");
        IPage<XxlJobLog> iPage = new Page<>(0, pagesize);
        iPage = page(iPage, queryWrapper);
//        queryWrapper.last(" limit " + pagesize);
        List<XxlJobLog> ilist = iPage.getRecords();
        return ilist.stream().map(XxlJobLog::getId).collect(Collectors.toList());
    }

    public boolean updateAlarmStatus(@Param("logId") long logId,
                                     @Param("oldAlarmStatus") int oldAlarmStatus,
                                     @Param("newAlarmStatus") int newAlarmStatus) {
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setAlarmStatus(newAlarmStatus);
        QueryWrapper<XxlJobLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(XxlJobLog::getId, logId)
                .eq(XxlJobLog::getAlarmStatus, oldAlarmStatus);
        return update(xxlJobLog, queryWrapper);
    }

    public List<Long> findLostJobIds(@Param("losedTime") Date losedTime) {
        List<XxlJobRegistry> registrs = xxlJobRegistryService.list();
        Set<String> address = registrs.stream().map(XxlJobRegistry::getRegistryValue).collect(Collectors.toSet());
        QueryWrapper<XxlJobLog> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<XxlJobLog> lambda = queryWrapper.lambda();
        if (!address.isEmpty()) {
            lambda.in(XxlJobLog::getExecutorAddress, address);
        }
        lambda.eq(XxlJobLog::getTriggerCode, 200).eq(XxlJobLog::getHandleCode, 0)
                .le(XxlJobLog::getTriggerTime, losedTime);
        List<XxlJobLog> ilist = list(queryWrapper);
        return ilist.stream().map(XxlJobLog::getId).collect(Collectors.toList());
    }
}
