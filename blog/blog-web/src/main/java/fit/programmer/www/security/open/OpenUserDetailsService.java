package fit.programmer.www.security.open;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import fit.programmer.www.entity.OpenUser;
import fit.programmer.www.entity.Role;
import fit.programmer.www.entity.User;
import fit.programmer.www.service.OpenUserService;
import fit.programmer.www.service.RoleService;
import fit.programmer.www.service.UserService;

import java.util.List;

/**
 * Created by wly on 2018/6/29.
 */
public class OpenUserDetailsService implements UserDetailsService {

	@Autowired
	private OpenUserService openUserService;
	@Autowired
	private UserService userService;
	@Autowired
	private RoleService roleService;

	public UserDetails loadUserByUsername(String openId) throws OpenUserNotFoundException {
		OpenUser openUser = openUserService.findByOpenId( openId);
		if(openUser == null){
			throw new OpenUserNotFoundException("第三方用户openId不存在");
		}
		User user = userService.findById( openUser.getuId() );//修改直接根据用户id查询
		List<Role> roles = roleService.findByUid( user.getId() );
		user.setRoles( roles );
		openUser.setUser( user );
		return openUser;
	}
}
