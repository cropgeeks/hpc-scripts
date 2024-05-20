<template>
  <v-card>
    <v-tabs v-model="viewMenu" density="compact" color="red accent-2">
      <v-tab class="text-lowercase" value="size">Size</v-tab>
      <v-tab class="text-lowercase" value="files">Files</v-tab>
      <v-tab class="text-lowercase" value="directories">Directories</v-tab>
    </v-tabs>

    <v-card-item density="compact">
      <v-window v-model="viewMenu">
        <v-window-item value="size" transition="false">
          <HomeSize v-bind:home="this.home"/>
        </v-window-item>

        <v-window-item value="files" transition="false">
          <HomeFiles v-bind:home="this.home"/>
        </v-window-item>

        <v-window-item value="directories" transition="false">
          <HomeDirs v-bind:home="this.home"/>
        </v-window-item>
      </v-window>
    </v-card-item>
  </v-card>
</template>

<script>
import HomeSize from '../components/HomeSize.vue'
import HomeFiles from '../components/HomeFiles.vue'
import HomeDirs from '../components/HomeDirs.vue'

export default {
  name: 'HomeCard',
  props: ['home'],
  
  components: {
    HomeSize,
    HomeFiles,
    HomeDirs
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