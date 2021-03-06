package fit.programmer.www.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import fit.programmer.www.dao.RoleMapper;
import fit.programmer.www.entity.Role;
import fit.programmer.www.service.RoleService;

import java.util.List;

/**
 * Created by wly on 2017/12/15.
 */
@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleMapper roleMapper;

    public Role findById(Long id) {
        Role role = new Role();
        role.setId( id );
        return roleMapper.selectOne( role );
    }

    @Override
    public int add(Role role) {
        return roleMapper.insert(role);
    }

    @Override
    public List<Role> findByUid(Long uid) {
        return roleMapper.findByUid(uid);
    }
}
