<template>
  <v-card density="compact">
    <v-tabs v-model="viewMenu" density="compact" color="red accent-2">
      <v-tab class="text-lowercase" value="size">Size</v-tab>
      <v-tab class="text-lowercase" value="files">Files</v-tab>
      <v-tab class="text-lowercase" value="directories">Directories</v-tab>
    </v-tabs>

    <v-card-item>
      <v-window v-model="viewMenu">
        <v-window-item value="size" transition="false">
          <ProjectsSize v-bind:projects="this.projects"/>
        </v-window-item>

        <v-window-item value="files" transition="false">
          <ProjectsFiles v-bind:projects="this.projects"/>
        </v-window-item>

        <v-window-item value="directories" transition="false">
          <ProjectsDirs v-bind:projects="this.projects"/>
        </v-window-item>
      </v-window>
    </v-card-item>
  </v-card>
</template>

<script>
import ProjectsSize from '../components/ProjectsSize.vue';
import ProjectsFiles from '../components/ProjectsFiles.vue';
import ProjectsDirs from '../components/ProjectsDirs.vue';

export default {
  name: 'ProjectsCard',
  props: ['projects'],

  components: {
    ProjectsSize,
    ProjectsFiles,
    ProjectsDirs
  },

  data: function() {
    return {
      viewMenu: 'size'
    }
  }
}
</script>

<style>

</style>