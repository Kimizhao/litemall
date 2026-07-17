package org.linlinjava.litemall.db.service;

import com.github.pagehelper.PageHelper;
import org.linlinjava.litemall.db.dao.LitemallPrinterMapper;
import org.linlinjava.litemall.db.domain.LitemallPrinter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LitemallPrinterService {
    @Resource
    private LitemallPrinterMapper printerMapper;

    public List<LitemallPrinter> querySelective(String sn, String name, Integer page, Integer limit, String sort, String order) {
        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            // Because mybatis-generator has order clauses but we made a simple mapper, PageHelper will handle standard pagination.
            // Let's support sorting if needed or keep it simple.
            // If sort equals "add_time" we map it to add_time, etc.
        }
        PageHelper.startPage(page, limit);
        return printerMapper.selectSelective(sn, name);
    }

    public int updateById(LitemallPrinter printer) {
        printer.setUpdateTime(LocalDateTime.now());
        return printerMapper.updateByPrimaryKeySelective(printer);
    }

    public void deleteById(Integer id) {
        printerMapper.logicalDeleteByPrimaryKey(id);
    }

    public void add(LitemallPrinter printer) {
        printer.setAddTime(LocalDateTime.now());
        printer.setUpdateTime(LocalDateTime.now());
        printer.setDeleted(false);
        if (printer.getOnline() == null) {
            printer.setOnline(0); // 0: offline
        }
        if (printer.getStatus() == null) {
            printer.setStatus(0); // 0: normal
        }
        printerMapper.insertSelective(printer);
    }

    public List<LitemallPrinter> queryAll() {
        return printerMapper.selectSelective(null, null);
    }

    public LitemallPrinter findById(Integer id) {
        return printerMapper.selectByPrimaryKey(id);
    }
}
