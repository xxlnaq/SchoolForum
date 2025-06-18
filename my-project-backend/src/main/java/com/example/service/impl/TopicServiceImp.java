package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.*;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.TopicDetailsVO;
import com.example.entity.vo.response.TopicPreViewVO;
import com.example.entity.vo.response.TopicTopVO;
import com.example.mapper.*;
import com.example.service.TopicService;
import com.example.utils.CacheUtils;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TopicServiceImp extends ServiceImpl<TopicMapper, Topic> implements TopicService {
    @Resource
    TopicTypeMapper mapper;

    @Resource
    CacheUtils cacheUtils;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountDetailsMapper accountDetailsMapper;

    @Resource
    AccountPrivacyMapper accountPrivacyMapper;

    @Resource
    TopicCommentMapper commentMapper;

    @Resource
    StringRedisTemplate template;

    @Resource
    FlowUtils flowUtils;
    private Set<Integer> types=null;

    @PostConstruct
    public void initTypes() {
        types=this.listTypes()
                .stream().map(TopicType::getId).collect(Collectors.toSet());
    }



    @Override
    public List<TopicType> listTypes() {
        return mapper.selectList(null);
    }

    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        if (!textLimitCheck(vo.getContent(),20000)) return "文章内容太多，发文失败";
        if (!types.contains(vo.getType())) return  "文章类型非法";
        String key= Const.FORUM_TOPIC_CREATE_COUNTER + uid;
        if(!flowUtils.limitPeriodCounterCheck(key,3,3600)) return "发文频繁请稍后再试";
        Topic topic=new Topic();
        BeanUtils.copyProperties(vo,topic);
        topic.setContent(vo.getContent().toJSONString());
        topic.setUid(uid);
        topic.setTime(new Date());
        if (this.save(topic)) {
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE+"*");
            return null;
        }
        else {
            return "内部错误请联系管理员";
        }
    }

    @Override
    public List<TopicPreViewVO> listTopicByPage(int pageNumber, int type) {
        String key=Const.FORUM_TOPIC_PREVIEW_CACHE +pageNumber +":"+type;
        List<TopicPreViewVO> list=cacheUtils.takeListFromCache(key,TopicPreViewVO.class);
        if (list!=null){
            System.out.println("我从缓存中获取了");
            return list;
        }
        Page<Topic> page=Page.of(pageNumber,10);
        if (type==0)
            baseMapper.selectPage(page, Wrappers.<Topic>query().orderByDesc("time"));//每页10条数据，当前页码的下标
        else
            baseMapper.selectPage(page,Wrappers.<Topic>query().eq("type",type).orderByDesc("time"));
        List<Topic> topics=page.getRecords();
        if (topics.isEmpty())return null;
        list=topics.stream().map(this::resolveToPreview).toList();//等价为test -> resolveToPreview(test)test 是流中的当前 Topic 对象。
        cacheUtils.saveListToCache(key,list,60);//这里是将分好页码的数据放入缓存中
        return  list;
    }

    @Override
    public List<TopicTopVO> listTopTopics() {
        List<Topic> topics=baseMapper.selectList(Wrappers.<Topic>query()
                .select("id","title","time").eq("top",1));
        return topics.stream().map(topic -> {
            TopicTopVO vo=new TopicTopVO();
            BeanUtils.copyProperties(topic,vo);
            return vo;
        }).toList();
    }

    @Override
    public TopicDetailsVO getTopic(int tid,int uid) {
        TopicDetailsVO vo=new TopicDetailsVO();
        Topic topic=baseMapper.selectById(tid);
        BeanUtils.copyProperties(topic,vo);
        TopicDetailsVO.Interact interact=new TopicDetailsVO.Interact(
                hasInteract(tid,uid,"like"),
                hasInteract(tid,uid,"collect")
        );
        vo.setInteract(interact);
        TopicDetailsVO.User user=new TopicDetailsVO.User();
        vo.setUser(this.fillUserDetailsByPrivacy(user, topic.getUid()));
        return vo;
    }

    @Override
    public void interact(Interact interact, boolean state) {
        String type=interact.getType();
        synchronized (type.intern()) {
            template.opsForHash().put(type,interact.toKey(),Boolean.toString(state));
            this.saveInteractSchedule(type);//开启定时任务
        }
    }

    @Override
    public List<TopicPreViewVO> listTopicCollects(int uid) {
        System.out.println(baseMapper.collectTopics(uid));
        return baseMapper.collectTopics(uid)
            .stream()
            .map(topic -> {
            TopicPreViewVO vo=new TopicPreViewVO();
            BeanUtils.copyProperties(topic,vo);
            return vo;
        }).toList();
    }

    @Override
    public String updateTopic(int uid, TopicUpdateVO vo) {
        if (!textLimitCheck(vo.getContent(),20000)) return "文章内容太多，发文失败";
        if (!types.contains(vo.getType())) return  "文章类型非法";
        baseMapper.update(null,Wrappers.<Topic>update()
                .eq("uid",uid)
                .eq("id",vo.getId())
                .set("title",vo.getTitle())
                .set("content",vo.getContent().toString())
                .set("type",vo.getType())
        );
        return null;

    }

    @Override
    public String creatComment(int uid, AddCommentVO vo) {
        if (!textLimitCheck(JSONObject.parseObject(vo.getContent()),2000)) return "文章评论内容太多，发文失败";
        String key=Const.FORUM_TOPIC_CREATE_COUNTER+uid;
        if(!flowUtils.limitPeriodCounterCheck(key,10,60))
            return "发表评论频繁，请稍后再试";
        TopicComment comment=new TopicComment();
        comment.setUid(uid);
        BeanUtils.copyProperties(vo,comment);
        comment.setTime(new Date());
        commentMapper.insert(comment);
         return null;
    }

    private  boolean hasInteract(int uid, int tid, String type){
        String key=tid+":"+uid;
        if(template.opsForHash().hasKey(type,key)){
            return  Boolean.parseBoolean(template.opsForHash().entries(type).get(key).toString());
        }
        return baseMapper.userInteractCount(uid, tid, type)>0;
    }

    private final  Map<String, Boolean> state=new HashMap<>();

    ScheduledExecutorService service= Executors.newScheduledThreadPool(2);

    private void saveInteractSchedule(String type){
        //创建三秒钟定时任务
            if (!state.getOrDefault(type,false)){
                state.put(type,true);
                service.schedule(()->{
                    this.saveInteract(type);
                    state.put(type,false);
                },3, TimeUnit.SECONDS);
            }
    }

    private void saveInteract(String type){
        synchronized (type.intern()) {
            List<Interact> check=new LinkedList<>();
            List<Interact> uncheck=new LinkedList<>();
            template.opsForHash().entries(type).forEach((k,v)->{
                  if (Boolean.parseBoolean(v.toString())) {
                      check.add(Interact.parseInteract(k.toString(),type));
                  } else   {
                    uncheck.add(Interact.parseInteract(k.toString(),type));
                  }
            });
            if (!check.isEmpty()) baseMapper.addInteract(check,type);
            if (!uncheck.isEmpty()) baseMapper.deleteInteract(uncheck,type);
            template.delete(type);
        }
    }

    private  <T> T fillUserDetailsByPrivacy(T target, int uid){
        AccountDetails details=accountDetailsMapper.selectById(uid);
        Account account = accountMapper.selectById(uid);
        AccountPrivacy accountPrivacy = accountPrivacyMapper.selectById(uid);
        String[] ignores = accountPrivacy.hiddenFields();
        BeanUtils.copyProperties(account,target,ignores);
        BeanUtils.copyProperties(details,target,ignores);
        return target;
    }

    private TopicPreViewVO resolveToPreview(Topic topic) {
        TopicPreViewVO vo=new TopicPreViewVO();
        BeanUtils.copyProperties(accountMapper.selectById(topic.getUid()),vo);
        BeanUtils.copyProperties(topic,vo);
        vo.setLike(baseMapper.interactCount(topic.getId(),"like"));
        vo.setCollect(baseMapper.interactCount(topic.getId(),"collect"));
        List<String> images=new ArrayList<>();
        StringBuilder previewText=new StringBuilder();
        JSONArray ops=JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
        for (Object op:ops) {
            Object insert=JSONObject.from(op).get("insert");
            if(insert instanceof  String text){
                if(previewText.length()>=300) continue;
                previewText.append(text);
            } else if (insert instanceof Map<?,?> map) {
                Optional.ofNullable(map.get("image"))
                        .ifPresent(obj ->images.add(obj.toString()));
            }
        }
        vo.setText(previewText.length()>300? previewText.substring(0,300):previewText.toString());
        vo.setImages(images);
        return vo;
    }

    private boolean textLimitCheck(JSONObject object,int max){
        if(object==null) return false;
        long length=0;
        for (Object op : object.getJSONArray("ops")) {
            length+=JSONObject.from(op).getString("insert").length();
            if (length>max) {
                System.out.println("我测尼玛"+length);
                return false;
            }
        }
        System.out.println("我测尼玛"+length);
        return true;
    }

}
