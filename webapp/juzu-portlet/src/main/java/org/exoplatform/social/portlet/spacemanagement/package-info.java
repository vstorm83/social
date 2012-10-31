@Application 
@Portlet 
@Bindings({
  @Binding(value = SpaceService.class, implementation = GateInMetaProvider.class),
  @Binding(value = GroupPrefs.class, implementation = GateInMetaProvider.class)
})
@Assets(
  scripts = {
    @Script(id = "jquery", src = "js/jquery-1.8.2.min.js"),
    @Script(src = "js/less-1.2.2.min.js", depends = "jquery"),
    @Script(src = "js/bootstrap.js", depends = "jquery"),
    @Script(src = "js/bootstrap-collapse.js", depends = "jquery"),
    @Script(src = "js/bootstrap-tooltip.js", depends = "jquery"),
    @Script(src = "js/bootstrap-popover.js", depends = "jquery"),
    @Script(src = "js/jquery.bgiframe-2.1.2.js", depends = "jquery"),
    @Script(src = "js/jquery-ui.min.js", depends = "jquery"),
    @Script(src = "js/jquery.jstree.js", depends = "jquery"),
    @Script(src = "js/space-management/spacemanagement.js", depends = "juzu.ajax")
  },
  stylesheets = {
    @Stylesheet(src = "css/gatein.less"),
    @Stylesheet(src = "css/space-management/space-management.less")
  }
)
package org.exoplatform.social.portlet.spacemanagement;

import juzu.Application;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.portlet.Portlet;

import org.exoplatform.social.core.space.GroupPrefs;
import org.exoplatform.social.providers.GateInMetaProvider;
import org.exoplatform.social.core.space.spi.SpaceService;