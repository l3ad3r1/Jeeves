package com.hermes.agent.data.jotter

import com.hermes.agent.data.tools.CreateNoteTool
import com.hermes.agent.data.tools.SearchNotesTool
import com.hermes.agent.domain.agent.AgentFeature
import com.hermes.agent.domain.agent.BackupContribution
import com.hermes.agent.domain.agent.NavEntry
import com.hermes.agent.domain.tool.Tool
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JotterFeature @Inject constructor(
    private val createNoteTool: CreateNoteTool,
    private val searchNotesTool: SearchNotesTool,
) : AgentFeature {
    override val id: String = "jotter"

    override fun tools(): List<Tool> = listOf(createNoteTool, searchNotesTool)

    override fun promptFragment(): String =
        "You can manage notes using create_note and search_notes."

    override fun backupContributions(): List<BackupContribution> = listOf(
        BackupContribution("jotter_notes", "Notes and notebook entries")
    )

    override fun entries(): List<NavEntry> = listOf(
        NavEntry("jotter_notes", "Notes")
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class JotterFeatureModule {
    @Binds
    @IntoSet
    abstract fun bindJotterFeature(feature: JotterFeature): AgentFeature
}
