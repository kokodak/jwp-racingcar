package racingcar.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import racingcar.domain.Car;
import racingcar.repository.dao.GameDao;
import racingcar.repository.dao.PositionDao;
import racingcar.repository.dao.UserDao;
import racingcar.repository.dao.WinnerDao;
import racingcar.repository.entity.GameEntity;
import racingcar.repository.entity.PositionEntity;
import racingcar.repository.entity.UserEntity;
import racingcar.repository.entity.WinnerEntity;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SaveRacingCarServiceTest {

    private static final int FIRST_GAME_ID = 1;

    private final SaveRacingCarService saveRacingCarService;
    private final UserDao userDao;
    private final GameDao gameDao;
    private final PositionDao positionDao;
    private final WinnerDao winnerDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void afterEach() {
        final List<String> truncateQueries = getTruncateQueries(jdbcTemplate);
        truncateTables(jdbcTemplate, truncateQueries);
    }

    private List<String> getTruncateQueries(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("SELECT Concat('TRUNCATE TABLE ', TABLE_NAME, ';') AS q FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'", String.class);
    }

    private void truncateTables(final JdbcTemplate jdbcTemplate, final List<String> truncateQueries) {
        execute(jdbcTemplate, "SET REFERENTIAL_INTEGRITY FALSE");
        truncateQueries.forEach(v -> execute(jdbcTemplate, v));
        execute(jdbcTemplate, "SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void execute(final JdbcTemplate jdbcTemplate, final String query) {
        jdbcTemplate.execute(query);
    }

    @Autowired
    public SaveRacingCarServiceTest(final SaveRacingCarService saveRacingCarService,
                                    final UserDao userDao,
                                    final GameDao gameDao,
                                    final PositionDao positionDao,
                                    final WinnerDao winnerDao) {
        this.saveRacingCarService = saveRacingCarService;
        this.userDao = userDao;
        this.gameDao = gameDao;
        this.positionDao = positionDao;
        this.winnerDao = winnerDao;
    }

    @Test
    void RacingCarResult를_이용해_정보들을_DB에_저장한다() {
        final String modi = "modi";
        final String kokodak = "koko";
        final int trialCount = 10;
        final int modiPosition = 7;
        final int kokodakPosition = 5;

        final List<String> winners = List.of(modi);
        final List<Car> cars = List.of(
                Car.of(modi, modiPosition),
                Car.of(kokodak, kokodakPosition)
        );
        final RacingCarResult racingCarResult = new RacingCarResult(winners, cars, trialCount);

        saveRacingCarService.saveRacingCarResult(racingCarResult);

        assertUser(modi, kokodak);
        assertGame(trialCount);
        assertPosition(modiPosition, kokodakPosition);
        assertWinner(modi);
    }

    private void assertUser(final String modi, final String kokodak) {
        final UserEntity modiEntity = userDao.findByName(modi);
        final UserEntity kokodakEntity = userDao.findByName(kokodak);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(modiEntity.getName()).isEqualTo(modi);
        softAssertions.assertThat(kokodakEntity.getName()).isEqualTo(kokodak);
        softAssertions.assertAll();
    }

    private void assertGame(final int trialCount) {
        final List<GameEntity> games = gameDao.findAll();
        final GameEntity gameEntity = games.get(0);

        assertThat(gameEntity.getTrialCount()).isEqualTo(trialCount);
    }

    private void assertPosition(final int modiPosition, final int kokodakPosition) {
        final List<PositionEntity> positionEntities = positionDao.findByGameId(FIRST_GAME_ID);

        final Set<Integer> positions = positionEntities.stream()
                .map(PositionEntity::getPosition)
                .collect(Collectors.toSet());

        final SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(positions.contains(modiPosition)).isTrue();
        softAssertions.assertThat(positions.contains(kokodakPosition)).isTrue();
        softAssertions.assertAll();
    }

    private void assertWinner(final String modi) {
        final List<WinnerEntity> winnerEntities = winnerDao.findByGameId(FIRST_GAME_ID);
        final WinnerEntity winnerEntity = winnerEntities.get(0);

        final long modiUserId = winnerEntity.getUserId();
        final UserEntity modiEntity = userDao.findById(modiUserId);

        assertThat(modiEntity.getName()).isEqualTo(modi);
    }
}
