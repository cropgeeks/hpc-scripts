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
          <ScratchSize v-bind:scratch="this.scratch"/>
        </v-window-item>

        <v-window-item value="files" transition="false">
          <ScratchFiles v-bind:scratch="this.scratch"/>
        </v-window-item>

        <v-window-item value="directories" transition="false">
          <ScratchDirs v-bind:scratch="this.scratch"/>
        </v-window-item>
      </v-window>
    </v-card-item>
  </v-card>
</template>

<script>
import ScratchSize from '../components/ScratchSize.vue';
import ScratchFiles from '../components/ScratchFiles.vue';
import ScratchDirs from '../components/ScratchDirs.vue';

export default {
  name: 'ScratchCard',
  props: ['scratch'],

  components: {
    ScratchSize,
    ScratchFiles,
    ScratchDirs
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