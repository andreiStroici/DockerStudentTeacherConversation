package com.sd.laborator.model

import java.time.LocalDateTime

// aceasta e calsa care imi pastteaza imaginile de docker
data class Image(
    val id:String, // id-ul containerului
    val name: String, // numele containerului
    val tag: String, // tag-ul containerului
    val digest: String, // acesta identifica imaginea completa
    // datele de creare si de update ale containerului
    // initializare pentru ca initial si update si ora de creare sunt identice
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    // lista de imagini
    val layers: List<ImageLayer> = emptyList()
)

// este necesar pentru ca imaginile de docker sunt pastrate in layere distincte
// fiecare layer descrie o actiune sau o modificare asupra imaginii de linux din container
data class ImageLayer(
    val digest: String, // identifica layer-ul pentru acecea imagine
    val size: Long
)
