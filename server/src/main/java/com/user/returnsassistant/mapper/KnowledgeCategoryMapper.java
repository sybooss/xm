package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.KnowledgeCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeCategoryMapper {
    @Select("""
            <script>
            select * from knowledge_category
            where 1=1
            <if test="enabled != null">and enabled=#{enabled}</if>
            order by sort_order asc, id asc
            </script>
            """)
    List<KnowledgeCategory> list(@Param("enabled") Integer enabled);

    @Select("select * from knowledge_category where id=#{id}")
    KnowledgeCategory getById(Long id);

    @Select("select count(*) from knowledge_doc where category_id=#{id} and deleted=0")
    long countDocs(Long id);

    @Delete("delete from knowledge_doc where category_id=#{id} and deleted=1")
    void deleteSoftDeletedDocs(Long id);

    @Insert("""
            insert into knowledge_category(parent_id, category_code, category_name, sort_order, enabled)
            values(coalesce(#{parentId}, 0), #{categoryCode}, #{categoryName}, coalesce(#{sortOrder}, 0), coalesce(#{enabled}, 1))
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(KnowledgeCategory category);

    @Update("""
            update knowledge_category
            set parent_id=coalesce(#{parentId}, 0),
                category_code=#{categoryCode},
                category_name=#{categoryName},
                sort_order=coalesce(#{sortOrder}, 0),
                enabled=coalesce(#{enabled}, 1)
            where id=#{id}
            """)
    void update(KnowledgeCategory category);

    @Delete("delete from knowledge_category where id=#{id}")
    void delete(Long id);
}
