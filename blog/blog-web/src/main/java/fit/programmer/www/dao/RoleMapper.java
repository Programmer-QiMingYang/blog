package fit.programmer.www.dao;

import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;
import fit.programmer.www.entity.Role;

import java.util.List;

public interface RoleMapper extends Mapper<Role>{
    /**
     * 根据用户id查询角色信息
     * @param uid
     * @return
     */
    List<Role> findByUid(@Param("uid") Long uid);
}