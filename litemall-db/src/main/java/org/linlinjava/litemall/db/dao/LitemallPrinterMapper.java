package org.linlinjava.litemall.db.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.linlinjava.litemall.db.domain.LitemallPrinter;

public interface LitemallPrinterMapper {
    int deleteByPrimaryKey(Integer id);

    int insertSelective(LitemallPrinter record);

    LitemallPrinter selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(LitemallPrinter record);

    int logicalDeleteByPrimaryKey(Integer id);

    List<LitemallPrinter> selectSelective(@Param("sn") String sn, @Param("name") String name);
}
