package fit.programmer.www.interceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import tk.mybatis.mapper.entity.Example;
import fit.programmer.www.common.PageHelper;
import fit.programmer.www.controller.BaseController;
import fit.programmer.www.dao.UserContentMapper;
import fit.programmer.www.entity.User;
import fit.programmer.www.entity.UserContent;
import javax.servlet.*;

import java.io.IOException;
import java.util.List;

/**
 * Created by wly on 2018/4/26.
 */
public class IndexJspFilter extends BaseController implements Filter{
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        User user = getCurrentUser();
        request.setAttribute("user",user);
        System.out.println("===========自定义过滤器==========");
        ServletContext context = request.getServletContext();
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
        UserContentMapper userContentMapper = ctx.getBean(UserContentMapper.class);
        PageHelper.startPage(null, null);//开始分页
        List<UserContent> list = userContentMapper.findByJoin(null);
        PageHelper.Page endPage = PageHelper.endPage();//分页结束
        request.setAttribute("page", endPage );
        chain.doFilter(request, response);
    }

    public void destroy() {

    }
}
