package com.sm.jeyz9.storemateapi.seeders;

import com.sm.jeyz9.storemateapi.models.Category;
import com.sm.jeyz9.storemateapi.models.ProductStatus;
import com.sm.jeyz9.storemateapi.models.ProductStatusName;
import com.sm.jeyz9.storemateapi.repository.CategoryRepository;
import com.sm.jeyz9.storemateapi.repository.ProductStatusRepository;
import com.sm.jeyz9.storemateapi.repository.RoleRepository;
import com.sm.jeyz9.storemateapi.models.Role;
import com.sm.jeyz9.storemateapi.models.RoleName;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final ProductStatusRepository productStatusRepository;

    public DatabaseSeeder(RoleRepository roleRepository, CategoryRepository categoryRepository, ProductStatusRepository productStatusRepository) {
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.productStatusRepository = productStatusRepository;
    }

    @Override
    public void run(String... args) {
        if(roleRepository.count() == 0L) {
            List<Role> roles = new ArrayList<>();
            roles.add(new Role(null, RoleName.ADMIN));
            roles.add(new Role(null, RoleName.MODERATOR));
            roles.add(new Role(null, RoleName.USER));
            roleRepository.saveAll(roles);
        }
        
        if(categoryRepository.count() == 0L) {
            List<Category> categories = new ArrayList<>();
            categories.add(new Category(null, "Promotion", LocalDateTime.now()));
            categories.add(new Category(null, "Soap", LocalDateTime.now()));
            categories.add(new Category(null, "Drinks", LocalDateTime.now()));
            categories.add(new Category(null, "Shampoo", LocalDateTime.now()));
            categoryRepository.saveAll(categories);
        }
        
        if(productStatusRepository.count() == 0L) {
            List<ProductStatus> statuses = new ArrayList<>();
            statuses.add(new ProductStatus(null, ProductStatusName.ACTIVE.toString()));
            statuses.add(new ProductStatus(null, ProductStatusName.INACTIVE.toString()));
            statuses.add(new ProductStatus(null, ProductStatusName.DELETED.toString()));
            productStatusRepository.saveAll(statuses);
        }
    }
}
