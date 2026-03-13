package com.example.myapitest.model

/**
 * Representa um objeto Carro simplificado, contendo apenas o identificador.
 *
 * @property id O identificador único do carro.
 */
data class Car(
    val id: String,
    val value: ItemCar
)

/**
 * Representa os detalhes de um item de carro retornado pela API.
 *
 * @property id O identificador único do item do carro.
 * @property imageUrl A URL da imagem do carro.
 * @property year O ano de fabricação ou modelo do carro.
 * @property name O nome ou modelo do carro.
 * @property licence A placa do carro.
 * @property place As coordenadas geográficas onde o carro está localizado.
 */
data class ItemCar(
    val id: String,
    val imageUrl: String,
    val year: String,
    val name: String,
    val licence: String,
    val place: ItemPlace
)

/**
 * Representa a localização geográfica de um carro.
 *
 * @property lat A latitude da localização.
 * @property long A longitude da localização.
 */
data class ItemPlace(
    val lat: Double,
    val long: Double
)
