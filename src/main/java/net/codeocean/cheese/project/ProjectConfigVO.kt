package net.codeocean.cheese.project

open class ProjectConfigVO {
    private var projectname: String? = null
    private var pkg: String? = null
    private var bindings: String? = null
    private var platform: String? = null
    private var ui: String? = null


    fun getProjectname(): String? {
        return projectname
    }

    fun setProjectname(projectname: String?) {
        this.projectname = projectname
    }

    fun getPkg(): String? {
        return pkg
    }

    fun setPkg(pkg: String?) {
        this.pkg = pkg
    }

    fun getBindings(): String? {
        return bindings
    }

    fun getPlatform(): String? {
        return platform
    }

    fun setBindings(type: String?) {
        this.bindings = type
    }

    fun setPlatform(type: String?) {
        this.platform = type
    }

    fun getUi(): String? {
        return ui
    }

    fun setUi(type: String?) {
        this.ui = type
    }


}