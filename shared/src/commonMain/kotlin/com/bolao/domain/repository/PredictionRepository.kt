package com.bolao.domain.repository

import com.bolao.domain.model.Prediction
import kotlinx.coroutines.flow.Flow

/**
 * Contrato do repositório de palpites.
 *
 * A implementação concreta escreverá e lerá palpites do nosso backend.
 * O backend é responsável por calcular os pontos ao final de cada partida.
 */
interface PredictionRepository {

    /**
     * Observa todos os palpites do usuário corrente para uma competição.
     * Emite atualizações conforme os pontos são calculados pelo backend.
     *
     * @param userId        ID do usuário autenticado.
     * @param competitionId ID da competição.
     */
    fun observePredictionsByUser(userId: String, competitionId: String): Flow<List<Prediction>>

    /**
     * Observa o palpite do usuário para uma partida específica.
     * Retorna null se o usuário ainda não palpitou.
     *
     * @param userId  ID do usuário autenticado.
     * @param matchId ID da partida.
     */
    fun observePredictionForMatch(userId: String, matchId: String): Flow<Prediction?>

    /**
     * Salva ou atualiza o palpite de um usuário para uma partida.
     * Deve lançar exceção se a janela de palpites já estiver fechada.
     *
     * @param prediction Objeto [Prediction] a ser persistido.
     * @return           O palpite salvo com o ID gerado pelo backend.
     */
    suspend fun savePrediction(prediction: Prediction): Result<Prediction>

    /**
     * Retorna o ranking de pontos de todos os usuários de um bolão.
     *
     * @param competitionId ID da competição / grupo do bolão.
     */
    fun observeLeaderboard(competitionId: String): Flow<List<Prediction>>

    /**
     * Retorna todos os palpites (snapshot) para uma lista de usuários.
     * Ideal para cruzar placares ao vivo com as apostas dos membros da liga.
     */
    suspend fun getPredictionsForUsers(userIds: List<String>): Result<List<Prediction>>
}
