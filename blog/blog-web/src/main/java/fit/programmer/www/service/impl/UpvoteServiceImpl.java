package fit.programmer.www.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import fit.programmer.www.dao.UpvoteMapper;
import fit.programmer.www.entity.Upvote;
import fit.programmer.www.service.UpvoteService;

/**
 * Created by wly on 2017/12/15.
 */
@Service
public class UpvoteServiceImpl implements UpvoteService {
    @Autowired
    private UpvoteMapper upvoteMapper;


    public Upvote findByUidAndConId(Upvote upvote) {
      return  upvoteMapper.selectOne( upvote);
    }

    public int add(Upvote upvote) {
        return upvoteMapper.insert( upvote );
    }

    public Upvote getByUid(Upvote upvote) {
        return null;
    }

    public void update(Upvote upvote) {
        upvoteMapper.updateByPrimaryKey( upvote );
    }

    public void deleteByContentId(Long cid) {
        Upvote upvote = new Upvote();
        upvote.setContentId(cid);
        upvoteMapper.delete(upvote);
    }
}
