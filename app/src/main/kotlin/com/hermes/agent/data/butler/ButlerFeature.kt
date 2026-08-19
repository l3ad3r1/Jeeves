package com.hermes.agent.data.butler

import com.hermes.agent.data.tools.SetAlarmTool
import com.hermes.agent.data.tools.TtsTool
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
class ButlerFeature @Inject constructor(
    private val setAlarmTool: SetAlarmTool,
    private val ttsTool: TtsTool,
) : AgentFeature {
    override val id: String = "butler"

    override fun tools(): List<Tool> = listOf(setAlarmTool, ttsTool)

    override fun promptFragment(): String =
        "You can manage alarms with set_alarm and speak responses with speak."

    override fun backupContributions(): List<BackupContribution> = listOf(
        BackupContribution("butler_alarms", "Alarms and schedules")
    )

    override fun entries(): List<NavEntry> = listOf(
        NavEntry("butler_alarms", "Alarms")
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ButlerFeatureModule {
    @Binds
    @IntoSet
    abstract fun bindButlerFeature(feature: ButlerFeature): AgentFeature
}
