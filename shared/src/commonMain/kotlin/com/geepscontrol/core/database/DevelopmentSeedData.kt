package com.geepscontrol.core.database

import com.geepscontrol.core.domain.Priority
import com.geepscontrol.core.domain.Project
import com.geepscontrol.core.domain.Task
import com.geepscontrol.core.domain.TaskContext
import com.geepscontrol.core.domain.TaskStatus
import com.geepscontrol.core.repository.ContextRepository
import com.geepscontrol.core.repository.FoundationRepository
import com.geepscontrol.core.repository.ProjectRepository
import com.geepscontrol.core.repository.TaskRepository
import kotlinx.coroutines.flow.first

class DevelopmentSeedData(
    private val foundationRepository: FoundationRepository,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
) {
    suspend fun insertOnce() {
        if (foundationRepository.observeMetadata().first().any { it.key == SEED_KEY }) return

        SAMPLE_PROJECTS.forEach { sample ->
            val projectId =
                projectRepository.save(
                    Project(
                        name = sample.name,
                        desiredOutcome = sample.outcome,
                        priority = sample.priority,
                    ),
                )
            sample.tasks.forEach { title ->
                taskRepository.save(
                    Task(
                        projectId = projectId,
                        title = title,
                        status = TaskStatus.READY,
                        priority = sample.priority,
                    ),
                )
            }
        }
        foundationRepository.put(SEED_KEY, "1")
    }

    private data class SampleProject(
        val name: String,
        val outcome: String,
        val priority: Priority,
        val tasks: List<String>,
    )

    private companion object {
        const val SEED_KEY = "development_seed_data_v1"
        val SAMPLE_PROJECTS =
            listOf(
                SampleProject(
                    "PhD Preparation",
                    "Build a sustainable preparation and study system",
                    Priority.HIGH,
                    listOf("Review Laplace-transform examples", "Review linear algebra fundamentals", "Read the course syllabus"),
                ),
                SampleProject(
                    "Cooling Vest",
                    "Produce a tested, documented cooling-vest prototype",
                    Priority.NORMAL,
                    listOf("Record pump test temperatures", "Compare tube thickness", "Document battery-runtime test"),
                ),
                SampleProject(
                    "Home Repairs",
                    "Complete the highest-impact home maintenance work",
                    Priority.NORMAL,
                    listOf("Seal patio-house joint", "Clear large weeds", "Organize workshop"),
                ),
            )
    }
}

suspend fun ensureDefaultContexts(repository: ContextRepository) {
    if (repository.observeContexts().first().isNotEmpty()) return
    listOf(
        "Anywhere",
        "Home",
        "Work",
        "Errands",
        "Computer",
        "Phone",
        "Workshop",
        "Quiet environment",
        "Internet required",
        "Offline capable",
        "Reading",
        "Writing",
        "Coding",
        "Physical activity",
    ).forEach { name -> repository.save(TaskContext(name = name, isSystemDefault = true)) }
}
