package net.codeocean.cheese.project


object DataState {

    private var projectConfigVO: ProjectConfigVO = ProjectConfigVO()

    fun getProjectConfigVO(): ProjectConfigVO {
        return projectConfigVO
    }

    fun setProjectConfigVO(projectConfigVO: ProjectConfigVO) {
        DataState.projectConfigVO = projectConfigVO
    }
}