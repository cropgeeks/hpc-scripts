<template>
      
  <b-row v-bind:style= "[node.online ? {} : { 'opacity': 0.3 }]">
    
    <b-col cols=12 md=4 lg=3>
      {{ node.name }}
    </b-col>
    
    <b-col class="pr-0" v-b-tooltip.hover :title="diskTerabytes">
      <b-progress class="position-relative" :max="node.diskTotal" show-progress>
        <b-progress-bar :value="node.diskUsed" :style="{ 'background-color': '#de6a6c', 'color': '#555559' }">
          <div class="justify-content-center d-flex position-absolute w-100">
            {{ diskPercent }}
          </div>
        </b-progress-bar>
      </b-progress>
    </b-col>
    
    <b-col class="pr-0" v-b-tooltip.hover :title="watts">
      <b-progress class="position-relative" :max="100" show-progress>
        <b-progress-bar :value="node.usage" :style="{ 'background-color': '#dea36a', 'color': '#555559' }">
          <div class="justify-content-center d-flex position-absolute w-100">
            {{ formatNum }}
          </div>
        </b-progress-bar>
      </b-progress>
    </b-col>

    <b-col v-b-tooltip.hover :title="memGigabytes">
      <b-progress class="position-relative" :max="node.memTotal" show-progress>
        <b-progress-bar :value="node.memUsed" :style="{ 'background-color': '#1abc9c', 'color': '#555559' }">
          <div class="justify-content-center d-flex position-absolute w-100">
            {{ memPercent }}
          </div>
        </b-progress-bar>
      </b-progress>
    </b-col>    
  
  </b-row>
  
</template>

<script>
export default {
  name: "StorageStats",
  props: {
    node: Object,
    showCPUs: null
  },

  computed: {
    watts() {
      if (this.node.online === false)
        return "";
      return this.node.watts.toLocaleString() + " watts";
    },
    memGigabytes() {
      if (this.node.online === false)
        return "";
      var usedGB = this.node.memUsed / 1024 / 1024;
      var totalGB = this.node.memTotal / 1024 / 1024;
      return usedGB.toFixed(1) + " GB / " + totalGB.toFixed(1) + " GB";
    },
    diskTerabytes() {
      if (this.node.online === false)
        return "";
      var usedTB = this.node.diskUsed / 1000 / 1000 / 1000 / 1000;
      var totalTB = this.node.diskTotal / 1000 / 1000 / 1000 / 1000;
      return usedTB.toFixed(1) + " TB / " + totalTB.toFixed(1) + " TB";
    },
    formatNum() {
      return this.node.usage.toFixed(1);
    },
    memPercent() {
      if (this.node.online)
        return ((this.node.memUsed / this.node.memTotal) * 100).toFixed(1);
      else
        return "0.0";
    },
    memAllocPercent() {
      if (this.node.online)
        return ((this.node.memAlloc / this.node.memTotal) * 100).toFixed(1);
      else
        return "0.0";
    },
    diskPercent() {
      if (this.node.online)
        return ((this.node.diskUsed / this.node.diskTotal) * 100).toFixed(1);
      else
        return "0.0";
    }
  }
}
</script>
