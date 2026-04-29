package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.KnowledgeDoc;
import com.user.returnsassistant.pojo.KnowledgeDocSearch;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper {
    @Select("""
            <script>
            select count(*)
            from knowledge_doc d
            where d.deleted=0
            <if test="s.categoryId != null">and d.category_id=#{s.categoryId}</if>
            <if test="s.docType != null and s.docType != ''">and d.doc_type=#{s.docType}</if>
            <if test="s.intentCode != null and s.intentCode != ''">and d.intent_code=#{s.intentCode}</if>
            <if test="s.status != null and s.status != ''">and d.status=#{s.status}</if>
            <if test="s.keyword != null and s.keyword != ''">
                and (d.title like concat('%',#{s.keyword},'%')
                 or d.question like concat('%',#{s.keyword},'%')
                 or d.content like concat('%',#{s.keyword},'%')
                 or d.keywords like concat('%',#{s.keyword},'%'))
            </if>
            </script>
            """)
    long count(@Param("s") KnowledgeDocSearch search);

    @Select("""
            <script>
            select d.*, c.category_name
            from knowledge_doc d
            left join knowledge_category c on d.category_id=c.id
            where d.deleted=0
            <if test="s.categoryId != null">and d.category_id=#{s.categoryId}</if>
            <if test="s.docType != null and s.docType != ''">and d.doc_type=#{s.docType}</if>
            <if test="s.intentCode != null and s.intentCode != ''">and d.intent_code=#{s.intentCode}</if>
            <if test="s.status != null and s.status != ''">and d.status=#{s.status}</if>
            <if test="s.keyword != null and s.keyword != ''">
                and (d.title like concat('%',#{s.keyword},'%')
                 or d.question like concat('%',#{s.keyword},'%')
                 or d.content like concat('%',#{s.keyword},'%')
                 or d.keywords like concat('%',#{s.keyword},'%'))
            </if>
            order by d.priority desc, d.updated_at desc, d.id desc
            limit #{s.offset}, #{s.limit}
            </script>
            """)
    List<KnowledgeDoc> page(@Param("s") KnowledgeDocSearch search);

    @Select("select d.*, c.category_name from knowledge_doc d left join knowledge_category c on d.category_id=c.id where d.id=#{id} and d.deleted=0")
    KnowledgeDoc getById(Long id);

    @Insert("""
            insert into knowledge_doc(category_id, title, doc_type, intent_code, scenario, question, answer, content, keywords, priority, status, version_no, created_by, updated_by, deleted)
            values(#{categoryId}, #{title}, #{docType}, #{intentCode}, #{scenario}, #{question}, #{answer}, #{content}, #{keywords}, coalesce(#{priority}, 0),
                   coalesce(#{status}, 'ENABLED'), coalesce(#{versionNo}, 1), #{createdBy}, #{updatedBy}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(KnowledgeDoc doc);

    @Update("""
            update knowledge_doc
            set category_id=#{categoryId},
                title=#{title},
                doc_type=#{docType},
                intent_code=#{intentCode},
                scenario=#{scenario},
                question=#{question},
                answer=#{answer},
                content=#{content},
                keywords=#{keywords},
                priority=coalesce(#{priority}, 0),
                status=coalesce(#{status}, 'ENABLED'),
                version_no=version_no + 1,
                updated_by=#{updatedBy}
            where id=#{id} and deleted=0
            """)
    void update(KnowledgeDoc doc);

    @Update("update knowledge_doc set deleted=1 where id=#{id}")
    void softDelete(Long id);

    @Select("""
            <script>
            select d.*, c.category_name,
                   0.8000 as score,
                   '命中标题、问题、正文或关键词' as hit_reason,
                   left(d.content, 120) as content_preview
            from knowledge_doc d
            left join knowledge_category c on d.category_id=c.id
            where d.deleted=0 and d.status='ENABLED'
            <if test="intentCode != null and intentCode != ''">
                and (d.intent_code=#{intentCode} or d.intent_code is null)
            </if>
            <if test="(intentCode == null or intentCode == '') and query != null and query != ''">
                and (d.title like concat('%',#{query},'%')
                 or d.question like concat('%',#{query},'%')
                 or d.content like concat('%',#{query},'%')
                 or d.keywords like concat('%',#{query},'%')
                 or #{query} like concat('%', d.title, '%'))
            </if>
            order by d.priority desc, d.updated_at desc, d.id desc
            limit #{limit}
            </script>
            """)
    List<KnowledgeDoc> search(@Param("query") String query, @Param("intentCode") String intentCode, @Param("limit") Integer limit);
}
