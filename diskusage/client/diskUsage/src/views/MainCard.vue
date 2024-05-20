<template>
  <v-card density="compact">
    <v-tabs v-model="folderMenu" density="compact" color="red accent-2">
      <v-tab value="home" >Home</v-tab>
      <v-tab value="projects">Projects</v-tab>
      <v-tab value="scratch">Scratch</v-tab>
    </v-tabs>

    <v-card-item class="pa-0">
      <v-window v-model="folderMenu">
        <v-window-item value="home" transition="false">
          <HomeCard v-bind:home="this.home"/>
        </v-window-item>

        <v-window-item value="projects" transition="false">
          <ProjectsCard v-bind:projects="this.projects"/>
        </v-window-item>

        <v-window-item value="scratch" transition="false">
          <ScratchCard v-bind:scratch="this.scratch"/>
        </v-window-item>
      </v-window>
    </v-card-item>
  </v-card>
</template>

<script>
import HomeCard from '../views/HomeCard.vue';
import ProjectsCard from '../views/ProjectsCard.vue'
import ScratchCard from '../views/ScratchCard.vue'

export default {
  name: 'MainCard',
  props: ['home', 'projects', 'scratch'],

  data: function() {
    return {
      folderMenu: "home"
    }
  },

  components: {
    HomeCard,
    ProjectsCard,
    ScratchCard
  }
}
</script>

<style>

</style>