package com.example.myapitest.model

/**
 * Representa os detalhes de um carro.
 *
 * @property id O identificador único do carro.
 * @property imageUrl A URL da imagem do carro.
 * @property year O ano de fabricação ou modelo do carro.
 * @property name O nome ou modelo do carro.
 * @property licence A placa do carro.
 * @property place A localização geográfica associada ao carro.
 */
data class Car(
    val id: String,
    val imageUrl: String,
    val year: String,
    val name: String,
    val licence: String,
    val place: ItemPlace
)

/**
 * Representa a localização geográfica (latitude e longitude).
 *
 * @property lat A latitude da localização.
 * @property long A longitude da localização.
 */
data class ItemPlace(
    val lat: Double,
    val long: Double
)
