// 文件：dfss-data/src/main/java/com/dfss/data/util/DataBaseOperation.java
package com.dfss.data.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dfss.exception.DataBaseErrorCode;
import com.dfss.exception.DataBaseOperationException;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>通用数据操作工具（静态门面 + Spring 上下文注入 + Mapper 缓存）。</p>
 *
 * <p>该类利用 MyBatis-Plus 的 BaseMapper 完成常用的增删改查操作，</p>
 * <p>并支持:</p>
 * <ul>
 *   <li>传入实体做 CRUD（insert/update/delete/select）。</li>
 *   <li>传入 VO 类做返回类型转换。</li>
 *   <li>传入 DTO 自动转换成实体后做查询/插入/更新等操作。</li>
 *   <li>统一抛出 {@link DataBaseOperationException}，结合 {@link DataBaseErrorCode} 进行错误分类。</li>
 * </ul>
 *
 * <p>典型调用示例：</p>
 * <pre>{@code
 * // 1. 传入实体直接插入
 * User user = new User().setName("Alice");
 * int rows = DataBaseOperation.insert(user);
 *
 * // 2. 传入 DTO 自动转换再插入
 * UserDTO dto = new UserDTO().setName("Bob");
 * DataBaseOperation.insertByDto(dto, User.class);
 *
 * // 3. 传入实体做条件查询并返回 VO 列表
 * User filter = new User().setRole("ADMIN");
 * List<UserVo> list = DataBaseOperation.list(filter, UserVo.class);
 *
 * // 4. 传入 DTO 做查询并返回单个 VO
 * UserDTO queryDto = new UserDTO().setId(100L);
 * UserVo vo = DataBaseOperation.getOneByDto(queryDto, User.class, UserVo.class);
 * }</pre>
 *
 * @author shushun
 * @since 2025-06-02
 */
@Component
public class DataBaseOperation implements ApplicationContextAware {
    /**
     * Spring 上下文，用于获取所有 BaseMapper Bean。
     */
    private static ApplicationContext context;

    /**
     * 缓存：实体类 -> 对应的 BaseMapper Bean 实例。
     */
    private static final Map<Class<?>, BaseMapper<?>> mapperCache = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        DataBaseOperation.context = applicationContext;
    }

    // ============================= private 辅助方法 =============================
    @SuppressWarnings("unchecked")
    private static <E> BaseMapper<E> getMapper(Class<E> entityClass) {
        if (entityClass == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY, "实体类不能为空。");
        }
        // 先从缓存里看一下
        BaseMapper<?> cached = mapperCache.get(entityClass);
        if (cached != null) {
            return (BaseMapper<E>) cached;
        }

        // 拿到 Spring 容器里所有 BaseMapper 类型的 Bean
        Map<String, BaseMapper> allMappers = context.getBeansOfType(BaseMapper.class);
        BaseMapper<?> foundMapper = null;

        // 遍历每个 Mapper Bean，尝试解析它实现的接口里，哪个接口的泛型参数等于 entityClass
        for (BaseMapper<?> mapperBean : allMappers.values()) {
            // 先把可能的代理剥离，拿到实际的目标类或接口类
            Class<?> proxyClass = AopUtils.getTargetClass(mapperBean);

            // 1. 找到“这个类实现了哪些接口”
            //    典型情况下，mapperBean 的目标类会直接实现 UserMapper（而 UserMapper extends BaseMapper<User>）
            for (Class<?> iface : proxyClass.getInterfaces()) {
                // 2. 我们只关心那些接口 “是 BaseMapper 的子类型” 的
                if (!BaseMapper.class.isAssignableFrom(iface)) {
                    continue;
                }

                // 3. 从这个接口 iface（通常是 UserMapper 接口）里再读取它的父接口签名
                //    （因为 UserMapper extends BaseMapper<User>，所以我们要在 GenericInterfaces 里找到 BaseMapper<User>）
                Type[] parentTypes = iface.getGenericInterfaces();
                for (Type t : parentTypes) {
                    if (!(t instanceof ParameterizedType)) {
                        continue;
                    }
                    ParameterizedType pt = (ParameterizedType) t;
                    // 只有当“父接口原始类型是 BaseMapper”时，我们才取它的泛型参数
                    if (!BaseMapper.class.equals(pt.getRawType())) {
                        continue;
                    }
                    // 取 BaseMapper 的泛型参数（应该只有一个：实体类型）
                    Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length != 1) {
                        continue;
                    }
                    if (!(typeArgs[0] instanceof Class<?>)) {
                        continue;
                    }
                    Class<?> mappedEntity = (Class<?>) typeArgs[0];
                    // 如果这个泛型参数就是我们要找的 entityClass，就说明找到了对应的 Mapper
                    if (entityClass.equals(mappedEntity)) {
                        // 如果之前已经找过一次了，就说明冲突了
                        if (foundMapper != null) {
                            throw new DataBaseOperationException(
                                    DataBaseErrorCode.DUPLICATE_MAPPER,
                                    "实体类 " + entityClass.getName() + " 存在多个 Mapper。"
                            );
                        }
                        foundMapper = mapperBean;
                    }
                }
            }
        }

        if (foundMapper == null) {
            throw new DataBaseOperationException(
                    DataBaseErrorCode.MAPPER_NOT_FOUND,
                    "未找到实体类 " + entityClass.getName() + " 对应的 Mapper。"
            );
        }

        // 缓存并返回
        mapperCache.put(entityClass, foundMapper);
        return (BaseMapper<E>) foundMapper;
    }
    /**
     * 创建目标类型实例（实体或 VO）。如果实例化失败，则抛出 DataBaseOperationException。
     *
     * @param clazz 要实例化的类
     * @param <T>   类型
     * @return 新实例
     */
    private static <T> T newInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY, "目标类不能为空。");
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new DataBaseOperationException(
                DataBaseErrorCode.ENTITY_INSTANTIATION_FAILED,
                "无法实例化 " + clazz.getName() + "，请检查是否存在公共无参构造函数。",
                ex
            );
        }
    }

    // ============================= 公共 CRUD 方法 =============================

    /**
     * 插入一条记录（传入实体）。
     *
     * @param entity 实体对象
     * @param <E>    实体类型
     * @return 受影响行数
     */
    public static <E> int insert(E entity) {
        if (entity == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>) entity.getClass();
        BaseMapper<E> mapper = getMapper(entityClass);
        return mapper.insert(entity);
    }

    /**
     * 插入一条记录（传入 DTO，自动转换成实体后插入）。
     *
     * @param dto         DTO 对象
     * @param entityClass 对应的实体类
     * @param <D>         DTO 类型
     * @param <E>         实体类型
     * @return 受影响行数
     */
    public static <D, E> int insertByDto(D dto, Class<E> entityClass) {
        if (dto == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        E entity = newInstance(entityClass);
        BeanUtils.copyProperties(dto, entity);
        return insert(entity);
    }

    /**
     * 根据实体 ID 删除一条记录。
     *
     * @param entityClass 实体类
     * @param id          主键 ID
     * @param <E>         实体类型
     * @return 受影响行数
     */
    public static <E> int deleteById(Class<E> entityClass, Serializable id) {
        if (entityClass == null || id == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        BaseMapper<E> mapper = getMapper(entityClass);
        return mapper.deleteById(id);
    }

    /**
     * 根据实体更新（主键 ID 必须在实体上）。
     *
     * @param entity 实体对象（必须包含主键值）
     * @param <E>    实体类型
     * @return 受影响行数
     */
    public static <E> int updateById(E entity) {
        if (entity == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>) entity.getClass();
        BaseMapper<E> mapper = getMapper(entityClass);
        return mapper.updateById(entity);
    }

    /**
     * 根据 DTO 更新（自动转换成实体后更新）。
     *
     * @param dto         DTO 对象（必须包含主键字段）
     * @param entityClass 对应的实体类
     * @param <D>         DTO 类型
     * @param <E>         实体类型
     * @return 受影响行数
     */
    public static <D, E> int updateByDto(D dto, Class<E> entityClass) {
        if (dto == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        E entity = newInstance(entityClass);
        BeanUtils.copyProperties(dto, entity);
        return updateById(entity);
    }

    /**
     * 根据主键查询一条记录。
     *
     * @param entityClass 实体类
     * @param id          主键 ID
     * @param <E>         实体类型
     * @return 查询到的实体对象，未找到则返回 null
     */
    public static <E> E getById(Class<E> entityClass, Serializable id) {
        if (entityClass == null || id == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        BaseMapper<E> mapper = getMapper(entityClass);
        return mapper.selectById(id);
    }

    /**
     * 根据实体条件查询一条记录并返回 VO。
     *
     * @param entity  实体对象（查询条件）
     * @param voClass VO 类型
     * @param <E>     实体类型
     * @param <V>     VO 类型
     * @return 转换后的 VO 对象；如果没有结果，返回 null；如果有多条，抛出异常
     */
    public static <E, V> V getOne(E entity, Class<V> voClass) {
        if (entity == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>) entity.getClass();
        BaseMapper<E> mapper = getMapper(entityClass);

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
     * 根据 DTO 条件查询一条记录并返回 VO。
     *
     * @param dto         DTO 对象（查询条件）
     * @param entityClass 对应的实体类
     * @param voClass     VO 类型
     * @param <D>         DTO 类型
     * @param <E>         实体类型
     * @param <V>         VO 类型
     * @return 转换后的 VO 对象；如果没有结果，返回 null；如果有多条，抛出异常
     */
    public static <D, E, V> V getOneByDto(D dto, Class<E> entityClass, Class<V> voClass) {
        if (dto == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        E entity = newInstance(entityClass);
        BeanUtils.copyProperties(dto, entity);
        return getOne(entity, voClass);
    }

    /**
     * 根据实体条件查询多条记录并返回实体列表。
     *
     * @param entity 实体对象（查询条件）
     * @param <E>    实体类型
     * @return 实体列表（如果无结果，返回空列表）
     */
    public static <E> List<E> listEntity(E entity) {
        if (entity == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>) entity.getClass();
        BaseMapper<E> mapper = getMapper(entityClass);

        LambdaQueryWrapper<E> wrapper = new LambdaQueryWrapper<>();
        wrapper.setEntity(entity);
        return mapper.selectList(wrapper);
    }

    /**
     * 根据实体条件查询多条记录并返回 VO 列表。
     *
     * @param entity  实体对象（查询条件）
     * @param voClass VO 类型
     * @param <E>     实体类型
     * @param <V>     VO 类型
     * @return VO 列表（如果无结果，返回空列表）
     */
    public static <E, V> List<V> list(E entity, Class<V> voClass) {
        List<E> entityList = listEntity(entity);
        // 实例化 VO 并复制属性
        return entityList.stream().map(record -> {
            V vo = newInstance(voClass);
            BeanUtils.copyProperties(record, vo);
            return vo;
        }).toList();
    }



    // ============================= 新增：MyBatis-Plus 原生分页 =============================

    /**
     * 分页查询实体列表，返回 IPage<E>。
     *
     * @param entity   实体对象（查询条件）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页大小
     * @param <E>      实体类型
     * @return IPage<E>，包含分页结果及分页信息
     */
    public static <E> IPage<E> pageEntity(E entity, long pageNum, long pageSize) {
        if (entity == null) {
            throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
        }
        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>) entity.getClass();
        BaseMapper<E> mapper = getMapper(entityClass);

        // 构造 Page 对象，并把 pageNum/pageSize 传进去
        Page<E> page = new Page<>(pageNum, pageSize);

        // 构造查询条件（等价于 where 字段 = 值），若想要更复杂的条件，可以由调用方传入 Wrapper
        LambdaQueryWrapper<E> wrapper = new LambdaQueryWrapper<>();
        wrapper.setEntity(entity);

        // 执行分页查询
        return mapper.selectPage(page, wrapper);
    }

    /**
     * 分页查询实体列表并转换为 VO 列表，返回 IPage<V>。
     *
     * @param entity   实体对象（查询条件）
     * @param voClass  VO 类型
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页大小
     * @param <E>      实体类型
     * @param <V>      VO 类型
     * @return IPage<V>，包含 VO 列表及分页信息
     */
    public static <E, V> IPage<V> page(E entity, Class<V> voClass, long pageNum, long pageSize) {
        // 先分页查询实体，得到 IPage<E>
        IPage<E> pageEntity = pageEntity(entity, pageNum, pageSize);

        // 把 E 列表转换为 V 列表
        List<E> records = pageEntity.getRecords();
        List<V> voList = records.stream().map(record -> {
            V vo = newInstance(voClass);
            BeanUtils.copyProperties(record, vo);
            return vo;
        }).toList();

        // 构造一个新的 Page<V>，并把分页信息拷贝过去
        Page<V> pageVo = new Page<>(pageEntity.getCurrent(), pageEntity.getSize(), pageEntity.getTotal());
        pageVo.setRecords(voList);
        pageVo.setPages(pageEntity.getPages());
        pageVo.setTotal(pageEntity.getTotal());
        pageVo.setCurrent(pageEntity.getCurrent());
        pageVo.setSize(pageEntity.getSize());
        pageVo.setRecords(voList);

        return pageVo;
    }
}
