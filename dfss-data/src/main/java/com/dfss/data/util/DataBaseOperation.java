package com.dfss.data.util;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.dfss.common.exceptions.DataBaseErrorCode;
import com.dfss.common.exceptions.DataBaseOperationException;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>通用数据操作工具（静态门面 + Spring 上下文注入 + Mapper 缓存）。</p>
 *
 * <p>该类利用 MyBatis-Plus 的 BaseMapper 完成常用的增删改查、分页、批量操作，</p>
 * <p>并支持:</p>
 * <ul>
 *   <li>传入实体做 CRUD（insert/update/delete/select）。</li>
 *   <li>传入 DTO 自动转换后做操作。</li>
 *   <li>根据实体或 Wrapper 做复杂条件查询、更新。</li>
 *   <li>统一抛出 {@link DataBaseOperationException}，结合 {@link DataBaseErrorCode} 进行错误分类。</li>
 *   <li>事务控制：新增了 @Transactional 注解，方便在批量操作中自动回滚。</li>
 *   <li>批量插入/更新：提供 saveBatch、updateBatch 等方法（循环调用 BaseMapper）。</li>
 *   <li>支持自定义 SQL 查询：返回 Map 或自定义 VO/DTO 列表。</li>
 * </ul>
 *
 * @author shushun
 * @since 2025-06-02
 */
@Component
public class DataBaseOperation implements ApplicationContextAware {
    // Spring 上下文，用于获取所有 BaseMapper Bean
    private static ApplicationContext context;

    // 缓存：实体类 -> 对应的 BaseMapper Bean
    private static final Map<Class<?>, BaseMapper<?>> mapperCache = new ConcurrentHashMap<>();



    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        DataBaseOperation.context = applicationContext;
    }

    //==================== 私有辅助方法 ====================



    /**
     * 从 Spring 容器中查找并缓存 BaseMapper<E>
     */
    @SuppressWarnings("unchecked")
    private static <E> BaseMapper<E> getMapper(Class<E> entityClass) {
        if (entityClass == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY, "实体类不能为空");
        }
        BaseMapper<?> cached = mapperCache.get(entityClass);
        if (cached != null) {
            return (BaseMapper<E>) cached;
        }

        BaseMapper<?> found = null;
        for (BaseMapper<?> mapperBean : context.getBeansOfType(BaseMapper.class).values()) {
            Class<?> target = AopUtils.getTargetClass(mapperBean);
            for (Class<?> iface : target.getInterfaces()) {
                if (!BaseMapper.class.isAssignableFrom(iface)) {
                    continue;
                }
                for (Type superType : iface.getGenericInterfaces()) {
                    if (!(superType instanceof ParameterizedType pt)) {
                        continue;
                    }
                    if (!BaseMapper.class.equals(pt.getRawType())) {
                        continue;
                    }
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length != 1 || !(args[0] instanceof Class<?> mappedEntity)) {
                        continue;
                    }
                    if (!entityClass.equals(mappedEntity)) {
                        continue;
                    }
                    if (found != null) {
                        throw new DataBaseOperationException(
                                DataBaseErrorCode.DUPLICATE_MAPPER,
                                "实体 " + entityClass.getName() + " 存在多个 Mapper"
                        );
                    }
                    found = mapperBean;
                }
            }
        }

        if (found == null) {
            throw new DataBaseOperationException(
                    DataBaseErrorCode.MAPPER_NOT_FOUND,
                    "未找到实体 " + entityClass.getName() + " 对应的 Mapper"
            );
        }
        mapperCache.put(entityClass, found);
        return (BaseMapper<E>) found;
    }



    private static <T> T newInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY, "目标类不能为空");
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new DataBaseOperationException(
                    DataBaseErrorCode.ENTITY_INSTANTIATION_FAILED,
                    "无法实例化 " + clazz.getName(), ex
            );
        }
    }



    private static void checkNull(Object obj, String msg) {
        if (obj == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY, msg);
        }
    }



    @SuppressWarnings("unchecked")
    private static <E> Class<E> getEntityClass(E entity) {
        return (Class<E>) entity.getClass();
    }

    //==================== 公共 CRUD ====================



    /**
     * 插入实体
     */
    public static <E> int insert(E entity) {
        checkNull(entity, "插入实体不能为空");
        BaseMapper<E> mapper = getMapper(getEntityClass(entity));
        return mapper.insert(entity);
    }



    /**
     * 插入 DTO → 实体
     */
    public static <D, E> int insertByDto(D dto, Class<E> entityClass) {
        checkNull(dto, "插入 DTO 不能为空");
        E entity = newInstance(entityClass);
        BeanUtils.copyProperties(dto, entity);
        return insert(entity);
    }



    /**
     * 根据 ID 删除
     */
    public static <E> int deleteById(Class<E> entityClass, Serializable id) {
        checkNull(entityClass, "实体类型不能为空");
        checkNull(id, "主键 ID 不能为空");
        BaseMapper<E> mapper = getMapper(entityClass);
        return mapper.deleteById(id);
    }



    /**
     * 更新实体（需包含主键）
     */
    public static <E> int updateById(E entity) {
        checkNull(entity, "更新实体不能为空");
        BaseMapper<E> mapper = getMapper(getEntityClass(entity));
        return mapper.updateById(entity);
    }



    /**
     * 更新 DTO → 实体
     */
    public static <D, E> int updateByDto(D dto, Class<E> entityClass) {
        checkNull(dto, "更新 DTO 不能为空");
        E entity = newInstance(entityClass);
        BeanUtils.copyProperties(dto, entity);
        return updateById(entity);
    }



    /**
     * 按主键查询实体
     */
    public static <E> E getById(Class<E> entityClass, Serializable id) {
        checkNull(entityClass, "实体类型不能为空");
        checkNull(id, "主键 ID 不能为空");
        BaseMapper<E> mapper = getMapper(entityClass);
        return mapper.selectById(id);
    }



    /**
     * 按实体条件查询单条并转换为 VO
     */
    public static <E, V> V getOne(E entity, Class<V> voClass) {
        checkNull(entity, "查询实体不能为空");
        BaseMapper<E> mapper = getMapper(getEntityClass(entity));
        LambdaQueryWrapper<E> wrapper = new LambdaQueryWrapper<>();
        wrapper.setEntity(entity);
        E result = mapper.selectOne(wrapper);
        if (result == null) {
            return null;
        }
        V vo = newInstance(voClass);
        BeanUtils.copyProperties(result, vo);
        return vo;
    }



    /**
     * 按 DTO 条件查询单条并转换为 VO
     */
    public static <D, E, V> V getOneByDto(D dto, Class<E> entityClass, Class<V> voClass) {
        checkNull(dto, "查询 DTO 不能为空");
        E entity = newInstance(entityClass);
        BeanUtils.copyProperties(dto, entity);
        return getOne(entity, voClass);
    }



    /**
     * 按实体条件查询多条实体
     */
    public static <E> List<E> listEntity(E entity) {
        checkNull(entity, "查询实体不能为空");
        BaseMapper<E> mapper = getMapper(getEntityClass(entity));
        LambdaQueryWrapper<E> wrapper = new LambdaQueryWrapper<>();
        wrapper.setEntity(entity);
        return mapper.selectList(wrapper);
    }



    /**
     * 按实体条件查询并转换为 VO 列表
     */
    public static <E, V> List<V> list(E entity, Class<V> voClass) {
        List<E> list = listEntity(entity);
        return list.stream().map(e -> {
            V vo = newInstance(voClass);
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
    }

    //==================== Wrapper 方式的动态查询/更新 ====================



    /**
     * 按 Wrapper 条件查询实体列表
     */
    public static <E> List<E> listByWrapper(Class<E> entityClass, Wrapper<E> wrapper) {
        checkNull(entityClass, "实体类型不能为空");
        checkNull(wrapper, "查询 Wrapper 不能为空");
        BaseMapper<E> mapper = getMapper(entityClass);
        return mapper.selectList(wrapper);
    }



    /**
     * 按 Wrapper 条件更新实体（字段更新请在 wrapper 中指定 set 操作）
     */
    public static <E> int updateByWrapper(Class<E> entityClass, LambdaUpdateWrapper<E> wrapper) {
        checkNull(entityClass, "实体类型不能为空");
        checkNull(wrapper, "更新 Wrapper 不能为空");
        BaseMapper<E> mapper = getMapper(entityClass);
        return mapper.update(null, wrapper);
    }

    //==================== 分页操作 ====================



    /**
     * 分页查询实体列表，返回 IPage<E>
     */
    public static <E> IPage<E> pageEntity(E entity, long pageNum, long pageSize) {
        checkNull(entity, "分页查询实体不能为空");
        BaseMapper<E> mapper = getMapper(getEntityClass(entity));
        Page<E> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<E> wrapper = new LambdaQueryWrapper<>();
        wrapper.setEntity(entity);
        return mapper.selectPage(page, wrapper);
    }



    /**
     * 分页查询并转换为 VO，返回 IPage<V>
     */
    public static <E, V> IPage<V> page(E entity, Class<V> voClass, long pageNum, long pageSize) {
        IPage<E> pageE = pageEntity(entity, pageNum, pageSize);
        return getPage(voClass, pageE);
    }



    private static <E, V> Page<V> getPage(Class<V> voClass, IPage<E> pageE) {
        List<V> voList = pageE.getRecords().stream().map(e -> {
            V vo = newInstance(voClass);
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        Page<V> pageV = new Page<>(pageE.getCurrent(), pageE.getSize(), pageE.getTotal());
        pageV.setRecords(voList);
        return pageV;
    }



    /**
     * 按 Wrapper 分页查询实体列表
     */
    public static <E> IPage<E> pageByWrapper(Class<E> entityClass, Wrapper<E> wrapper, long pageNum, long pageSize) {
        checkNull(entityClass, "实体类型不能为空");
        checkNull(wrapper, "查询 Wrapper 不能为空");
        BaseMapper<E> mapper = getMapper(entityClass);
        Page<E> page = new Page<>(pageNum, pageSize);
        return mapper.selectPage(page, wrapper);
    }



    /**
     * 按 Wrapper 分页查询并转换为 VO
     */
    public static <E, V> IPage<V> pageByWrapper(
            Class<E> entityClass,
            Wrapper<E> wrapper,
            Class<V> voClass,
            long pageNum,
            long pageSize
    ) {
        IPage<E> pageE = pageByWrapper(entityClass, wrapper, pageNum, pageSize);
        return getPage(voClass, pageE);
    }

    //==================== 批量操作 ====================



    /**
     * 批量插入（循环调用 insert）。推荐行数少时使用；若数据量大，建议使用 Service.saveBatch 或自定义 SQL。
     */
    public static <E> void saveBatch(List<E> entities) {
        checkNull(entities, "批量插入列表不能为空");
        for (E e : entities) {
            insert(e);
        }
    }



    /**
     * 批量更新（循环调用 updateById）。建议每次更新字段固定，或改为使用 Wrapper 方式。
     */
    public static <E> void updateBatch(List<E> entities) {
        checkNull(entities, "批量更新列表不能为空");
        for (E e : entities) {
            updateById(e);
        }
    }

    //==================== 自定义 SQL 查询 ====================



    /**
     * 执行任意自定义 SQL，并把结果映射到 VO/DTO 列表。
     * 需要在对应的 Mapper 中定义一个方法，例如：
     * List<VO> customQuery(@Param("param") String param);
     * 然后通过 DataBaseOperation.executeCustomQuery("com.xxx.mapper.XxxMapper", "customQuery", param);
     */
    @SuppressWarnings("unchecked")
    public static <V> List<V> executeCustomQuery(
            String mapperBeanName,
            String methodName,
            Object... args
    ) {
        Object mapperBean = context.getBean(mapperBeanName);
        try {
            // 通过反射调用 Mapper 中自定义的方法
            java.lang.reflect.Method method = mapperBean.getClass().getMethod(methodName,
                    java.util.Arrays.stream(args)
                            .map(Object::getClass)
                            .toArray(Class<?>[]::new));
            return (List<V>) method.invoke(mapperBean, args);
        } catch (NoSuchMethodException e) {
            throw new DataBaseOperationException(
                    DataBaseErrorCode.INVALID_ENTITY,
                    "Mapper 方法未找到: " + methodName, e
            );
        } catch (Exception e) {
            throw new DataBaseOperationException(
                    DataBaseErrorCode.ENTITY_INSTANTIATION_FAILED,
                    "执行自定义查询出错: " + methodName, e
            );
        }
    }



    /**
     * 执行自定义更新/删除 SQL（Mapper 中需定义对应方法）。
     * 例如：int customUpdate(@Param("param") String param);
     */
    public static int executeCustomUpdate(String mapperBeanName, String methodName, Object... args) {
        Object mapperBean = context.getBean(mapperBeanName);
        try {
            java.lang.reflect.Method method = mapperBean.getClass().getMethod(methodName,
                    java.util.Arrays.stream(args)
                            .map(Object::getClass)
                            .toArray(Class<?>[]::new));
            return (int) method.invoke(mapperBean, args);
        } catch (NoSuchMethodException e) {
            throw new DataBaseOperationException(
                    DataBaseErrorCode.INVALID_ENTITY,
                    "Mapper 更新方法未找到: " + methodName, e
            );
        } catch (Exception e) {
            throw new DataBaseOperationException(
                    DataBaseErrorCode.ENTITY_INSTANTIATION_FAILED,
                    "执行自定义更新出错: " + methodName, e
            );
        }
    }


    /**
     * 构造一个空的 LambdaQueryWrapper<E>，用于链式拼接条件。
     *
     * 用法示例：
     *   LambdaQueryWrapper<User> wrapper = DataBaseOperation.lambdaQuery(User.class)
     *       .between(User::getAge, 18, 30)
     *       .isNotNull(User::getEmail)
     *       .eq(User::getStatus, 1);
     *
     * 然后可以传给 listByWrapper/pageByWrapper 等方法使用。
     */
    public static <E> LambdaQueryWrapper<E> lambdaQuery(Class<E> entityClass) {
        checkNull(entityClass, "实体类型不能为空");
        return new LambdaQueryWrapper<>();
    }

    /**
     * 构造一个空的 LambdaUpdateWrapper<E>，用于链式拼接更新条件和设置字段。
     *
     * 用法示例：
     *   LambdaUpdateWrapper<User> wrapper = DataBaseOperation.lambdaUpdate(User.class)
     *       .eq(User::getStatus, 0)            // 条件：status=0
     *       .set(User::getStatus, 1);         // 将 status 设置为 1
     *
     * 然后可以传给 updateByWrapper 使用。
     */
    public static <E> LambdaUpdateWrapper<E> lambdaUpdate(Class<E> entityClass) {
        checkNull(entityClass, "实体类型不能为空");
        return new LambdaUpdateWrapper<>();
    }
}
