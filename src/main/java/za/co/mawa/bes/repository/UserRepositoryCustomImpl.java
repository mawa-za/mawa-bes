package za.co.mawa.bes.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import za.co.mawa.bes.dto.RoleDto;
import za.co.mawa.bes.entity.UserRoleEntity;

import java.util.List;

public class UserRepositoryCustomImpl implements UserRepositoryCustom{
    @PersistenceContext
    EntityManager entityManager;

    @Override
    public List<RoleDto> findRoleByUser(String user) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserRoleEntity> query = cb.createQuery(UserRoleEntity.class);
        Root<UserRoleEntity> userRoleEntityRoot = query.from(UserRoleEntity.class);
//        query.select(userRoleEntityRoot)
//                .where(cb.equal());
//
//        return entityManager.createQuery(query)
//                .getResultList();
        return null;
    }
}
