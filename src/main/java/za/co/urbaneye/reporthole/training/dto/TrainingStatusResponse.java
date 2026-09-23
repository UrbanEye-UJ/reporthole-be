package za.co.urbaneye.reporthole.training.dto;

import za.co.urbaneye.reporthole.training.entity.TrainingStatus;

import java.util.UUID;

public record TrainingStatusResponse(UUID incidentId, TrainingStatus trainingStatus) {}
