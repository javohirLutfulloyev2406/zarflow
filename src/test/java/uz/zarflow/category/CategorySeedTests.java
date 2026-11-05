package uz.zarflow.category;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import uz.zarflow.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CategorySeedTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedsExactlyFifteenExpenseAndEightIncomeSystemCategories() {
        Integer expenseCount = jdbcTemplate.queryForObject(
                "select count(*) from categories where user_id is null and type = 'EXPENSE'", Integer.class);
        Integer incomeCount = jdbcTemplate.queryForObject(
                "select count(*) from categories where user_id is null and type = 'INCOME'", Integer.class);

        assertThat(expenseCount).isEqualTo(15);
        assertThat(incomeCount).isEqualTo(8);
    }

    @Test
    void everyExpenseCategoryHasNecessityAndEveryIncomeCategoryDoesNot() {
        Integer expenseWithoutNecessity = jdbcTemplate.queryForObject(
                "select count(*) from categories where user_id is null and type = 'EXPENSE' and necessity is null",
                Integer.class);
        Integer incomeWithNecessity = jdbcTemplate.queryForObject(
                "select count(*) from categories where user_id is null and type = 'INCOME' and necessity is not null",
                Integer.class);

        assertThat(expenseWithoutNecessity).isZero();
        assertThat(incomeWithNecessity).isZero();
    }

    @Test
    void findVisibleForUserReturnsSystemCategoriesPlusOwnActiveOnes() {
        UUID userId = jdbcTemplate.queryForObject(
                "insert into users (telegram_id) values (?) returning id",
                UUID.class,
                ThreadLocalRandom.current().nextLong());

        categoryRepository.save(new Category(userId, null, "Mening kategoriyam", "Моя категория",
                CategoryType.EXPENSE, Necessity.DISCRETIONARY, "tag", "#9AA7B8", new String[0], 99));

        List<Category> visible = categoryRepository.findVisibleForUser(userId);

        assertThat(visible).hasSize(23 + 1);
        assertThat(visible).anyMatch(c -> "Mening kategoriyam".equals(c.getNameUz()));
    }

}
